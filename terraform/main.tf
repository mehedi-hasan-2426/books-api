terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# ── Networking ────────────────────────────────────────────────────────────────

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# ── Security Groups ───────────────────────────────────────────────────────────

resource "aws_security_group" "alb" {
  name        = "${var.app_name}-alb-sg"
  description = "Allow HTTP/HTTPS inbound to ALB"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "app" {
  name        = "${var.app_name}-app-sg"
  description = "Allow app traffic only from ALB, SSH from trusted IPs"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Spring Boot port from ALB only"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  ingress {
    description = "SSH - replace with your IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_cidr]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.app_name}-rds-sg"
  description = "Allow PostgreSQL only from app EC2"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "PostgreSQL from app only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }
}

# ── Secrets in SSM Parameter Store (encrypted) ───────────────────────────────

resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%^&*-_=+"
}

resource "aws_ssm_parameter" "jwt_secret" {
  name        = "/${var.app_name}/jwt-secret"
  type        = "SecureString"
  value       = var.jwt_secret
  description = "JWT signing secret for ${var.app_name}"
}

resource "aws_ssm_parameter" "db_password" {
  name        = "/${var.app_name}/db-password"
  type        = "SecureString"
  value       = random_password.db_password.result
  description = "RDS password for ${var.app_name}"
}

resource "aws_ssm_parameter" "database_url" {
  name        = "/${var.app_name}/database-url"
  type        = "String"
  value       = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${var.db_name}"
  description = "JDBC URL for ${var.app_name}"
}

# ── IAM Role: EC2 can read SSM + ECR pull ────────────────────────────────────

resource "aws_iam_role" "ec2_role" {
  name = "${var.app_name}-ec2-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "ec2_ssm_ecr" {
  name = "${var.app_name}-ssm-ecr-policy"
  role = aws_iam_role.ec2_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ssm:GetParameter", "ssm:GetParameters"]
        Resource = "arn:aws:ssm:${var.aws_region}:*:parameter/${var.app_name}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.app_name}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ── ECR Repository ───────────────────────────────────────────────────────────

resource "aws_ecr_repository" "books_api" {
  name                 = var.app_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

# ── RDS PostgreSQL ───────────────────────────────────────────────────────────

resource "aws_db_instance" "postgres" {
  identifier             = "${var.app_name}-db"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = var.db_instance_class
  allocated_storage      = 20
  db_name                = var.db_name
  username               = var.db_username
  password               = random_password.db_password.result
  vpc_security_group_ids = [aws_security_group.rds.id]
  skip_final_snapshot    = false
  final_snapshot_identifier = "${var.app_name}-final-snapshot"
  storage_encrypted      = true
  deletion_protection    = true
  backup_retention_period = 7
  multi_az               = false  # set true for production HA

  tags = {
    Name = "${var.app_name}-db"
  }
}

# ── EC2 Key Pair ─────────────────────────────────────────────────────────────

resource "aws_key_pair" "app_key" {
  key_name   = "${var.app_name}-key"
  public_key = file(var.public_key_path)
}

# ── EC2 Instance ─────────────────────────────────────────────────────────────

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.ec2_instance_type
  key_name               = aws_key_pair.app_key.key_name
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = <<-SCRIPT
    #!/bin/bash
    set -e
    yum update -y
    yum install -y docker awscli
    systemctl enable docker && systemctl start docker

    # Pull secrets from SSM (never hardcoded in user_data)
    JWT_SECRET=$(aws ssm get-parameter --name /${var.app_name}/jwt-secret \
      --with-decryption --query Parameter.Value --output text --region ${var.aws_region})
    DB_PASS=$(aws ssm get-parameter --name /${var.app_name}/db-password \
      --with-decryption --query Parameter.Value --output text --region ${var.aws_region})
    DB_URL=$(aws ssm get-parameter --name /${var.app_name}/database-url \
      --query Parameter.Value --output text --region ${var.aws_region})

    # Login to ECR and run
    aws ecr get-login-password --region ${var.aws_region} \
      | docker login --username AWS --password-stdin ${aws_ecr_repository.books_api.repository_url}

    docker run -d --name books-api \
      --restart unless-stopped \
      -p 8080:8080 \
      -e JWT_SECRET="$JWT_SECRET" \
      -e DATABASE_URL="$DB_URL" \
      -e DATABASE_USERNAME=${var.db_username} \
      -e DATABASE_PASSWORD="$DB_PASS" \
      -e SPRING_PROFILES_ACTIVE=prod \
      ${aws_ecr_repository.books_api.repository_url}:latest
  SCRIPT

  tags = {
    Name = "${var.app_name}-ec2"
  }

  depends_on = [aws_db_instance.postgres]
}

# ── Application Load Balancer ─────────────────────────────────────────────────

resource "aws_lb" "app" {
  name               = "${var.app_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids
}

resource "aws_lb_target_group" "app" {
  name     = "${var.app_name}-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id

  health_check {
    path                = "/api-docs"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
  }
}

resource "aws_lb_target_group_attachment" "app" {
  target_group_arn = aws_lb_target_group.app.arn
  target_id        = aws_instance.app.id
  port             = 8080
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
