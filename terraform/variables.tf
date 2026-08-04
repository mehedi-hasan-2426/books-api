variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Application name (used for resource naming)"
  type        = string
  default     = "books-api"
}

variable "ec2_instance_type" {
  description = "EC2 instance type (t3.small minimum for Java)"
  type        = string
  default     = "t3.small"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "booksdb"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "books_user"
}

variable "public_key_path" {
  description = "Path to your SSH public key"
  type        = string
  default     = "~/.ssh/books-api-key.pub"
}

variable "ssh_allowed_cidr" {
  description = "CIDR allowed to SSH — set to YOUR IP e.g. 203.0.113.5/32"
  type        = string
  default     = "0.0.0.0/0"  # restrict this in production!
}

variable "jwt_secret" {
  description = "JWT signing secret — pass via TF_VAR_jwt_secret env var, never hardcode"
  type        = string
  sensitive   = true
}
