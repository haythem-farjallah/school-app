@echo off
echo 🚀 Starting School Management System...
start powershell -Command "cd frontend; Write-Host '🌐 Frontend Starting...' -ForegroundColor Yellow; npm run dev"
timeout /t 2 /nobreak >nul
start powershell -Command "cd backend; Write-Host '⚙️ Backend Starting...' -ForegroundColor Yellow; mvn spring-boot:run"
echo ✅ Both services starting...
echo 🌐 Frontend: http://localhost:5173
echo ⚙️ Backend: http://localhost:8088
pause
