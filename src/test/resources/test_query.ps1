[void][System.Reflection.Assembly]::LoadWithPartialName("System.Data")
$connStr = "Server=localhost;Port=3306;Database=pagos_db;Uid=user;Pwd=user123;"
# Try finding mysql driver or using standard ODBC / Web endpoint
Write-Output "Checking endpoints..."
