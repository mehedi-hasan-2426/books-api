output "alb_dns_name" {
  description = "Load balancer DNS — use this as your API base URL"
  value       = "http://${aws_lb.app.dns_name}"
}

output "ec2_public_ip" {
  description = "EC2 public IP for SSH"
  value       = aws_instance.app.public_ip
}

output "ecr_repository_url" {
  description = "ECR URL — push your Docker image here"
  value       = aws_ecr_repository.books_api.repository_url
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}

output "ssm_jwt_secret_name" {
  description = "SSM parameter name for JWT secret"
  value       = aws_ssm_parameter.jwt_secret.name
}
