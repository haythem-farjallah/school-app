# School Management System - Development Startup Script
Write-Host "🚀 Starting School Management System..." -ForegroundColor Green

# Start Frontend in new PowerShell window
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; Write-Host '🌐 Frontend Starting...' -ForegroundColor Yellow; npm run dev"

# Wait 2 seconds
Start-Sleep -Seconds 2

# Start Backend in new PowerShell window
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; Write-Host '⚙️ Backend Starting...' -ForegroundColor Yellow; mvn spring-boot:run"

Write-Host "✅ Two PowerShell windows opened!" -ForegroundColor Green
Write-Host "🌐 Frontend: http://localhost:5173" -ForegroundColor Cyan
Write-Host "⚙️ Backend: http://localhost:8088" -ForegroundColor Cyan
