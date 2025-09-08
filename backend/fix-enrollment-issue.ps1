# PowerShell script to fix the enrollment enum issue

Write-Host "=== Fixing Enrollment Enum Issue ===" -ForegroundColor Green

# Step 1: Clean up failed Flyway migration
Write-Host "Step 1: Cleaning up failed Flyway migration..." -ForegroundColor Yellow
$env:PGPASSWORD = "postgres"
psql -h localhost -U postgres -d school_db -c "DELETE FROM flyway_schema_history WHERE version = '45';"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Successfully cleaned up failed migration" -ForegroundColor Green
} else {
    Write-Host "Failed to clean up migration. You may need to do this manually." -ForegroundColor Red
    Write-Host "Run this SQL manually: DELETE FROM flyway_schema_history WHERE version = '45';" -ForegroundColor Yellow
}

# Step 2: Restart the application
Write-Host "Step 2: Please restart the Spring Boot application to apply the new migration V46" -ForegroundColor Yellow
Write-Host "The new migration will safely convert the enum column to VARCHAR for JPA converter compatibility." -ForegroundColor Cyan

Write-Host "=== Next Steps ===" -ForegroundColor Green
Write-Host "1. Restart your Spring Boot application" -ForegroundColor White
Write-Host "2. Check the logs to ensure V46 migration runs successfully" -ForegroundColor White
Write-Host "3. Test the auto enrollment functionality" -ForegroundColor White

Write-Host "=== Fix Summary ===" -ForegroundColor Green
Write-Host "- Created JPA converter for EnrollmentStatus enum" -ForegroundColor White
Write-Host "- Updated Enrollment entity to use Convert annotation" -ForegroundColor White
Write-Host "- New migration V46 will convert database column to VARCHAR" -ForegroundColor White
Write-Host "- This avoids PostgreSQL enum comparison issues" -ForegroundColor White
