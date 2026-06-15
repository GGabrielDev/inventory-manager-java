#!/bin/bash
# setup-server.sh — Complete server setup for inventory-manager-java
# Run this on Debian 13 (Trixie) as root or with sudo
#
# One-liner:
#   curl -sL https://raw.githubusercontent.com/GGabrielDev/inventory-manager-java/refs/heads/master/setup-server.sh | bash
#
# Or copy and run locally:
#   sudo bash setup-server.sh
#
# NOTE: This script auto-generates secure random passwords on first run
# and prints them at the end. Save them somewhere safe!

set -euo pipefail

# ─── Auto-generate secure passwords ────────────────────────────────────
CONFIG_FILE="/opt/inventory-manager/.setup-credentials"
if [ -f "$CONFIG_FILE" ]; then
  source "$CONFIG_FILE"
  echo "📂 Using existing credentials from ${CONFIG_FILE}"
else
  DB_PASS=$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)
  JWT_SECRET=$(openssl rand -base64 48 | tr -d '/+=' | cut -c1-48)
  ADMIN_PASSWORD=$(openssl rand -base64 12 | tr -d '/+=' | cut -c1-16)
fi

# ─── Configuration ─────────────────────────────────────────────────────
APP_DIR="/opt/inventory-manager"
PROD_DIR="${APP_DIR}/prod"
DEMO_DIR="${APP_DIR}/demo"
PROD_PORT=4002
DEMO_PORT=4003
PROD_DB="inventory_prod"
DEMO_DB="inventory_demo"
DB_USER="inventory_user"
ADMIN_USERNAME="admin"

echo "================================================"
echo " Inventory Manager — Full Server Setup"
echo "================================================"

# ─── 1. Java 21 ─────────────────────────────────────────────────────────
echo ""
echo "📦 Installing Java 21 + PostgreSQL..."
apt-get update -qq
apt-get install -y -qq openjdk-21-jre-headless postgresql postgresql-client curl
java --version | head -1

# ─── 2. PostgreSQL ──────────────────────────────────────────────────────
echo ""
echo "🗄️  Setting up PostgreSQL databases..."
systemctl start postgresql
systemctl enable postgresql

su - postgres -c "psql -tc \"SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'\" | grep -q 1 || createuser ${DB_USER}"
su - postgres -c "psql -c \"ALTER USER ${DB_USER} WITH PASSWORD '${DB_PASS}'\""

for DB in $PROD_DB $DEMO_DB; do
  su - postgres -c "psql -tc \"SELECT 1 FROM pg_database WHERE datname='${DB}'\" | grep -q 1 || createdb -O ${DB_USER} ${DB}"
  su - postgres -c "psql -c \"GRANT ALL PRIVILEGES ON DATABASE ${DB} TO ${DB_USER}\""
  su - postgres -c "psql -d ${DB} -c \"GRANT ALL ON SCHEMA public TO ${DB_USER}\""
done

echo "  ✓ Databases: ${PROD_DB}, ${DEMO_DB}"

# ─── 3. Directories ─────────────────────────────────────────────────────
echo ""
echo "📁 Creating directories..."
mkdir -p "${PROD_DIR}" "${DEMO_DIR}"

# ─── 4. Config files ────────────────────────────────────────────────────
echo ""
echo "🔧 Writing per-environment config files..."

# Production config
cat > "${PROD_DIR}/application.properties" << PRODCFG
server.port=${PROD_PORT}
spring.datasource.url=jdbc:postgresql://localhost:5432/${PROD_DB}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
app.jwt.secret=${JWT_SECRET}
app.jwt.expire-minutes=480
app.cors-origin=*
PRODCFG

# Demo config (different JWT secret to keep sessions separate)
cat > "${DEMO_DIR}/application.properties" << DEMOCFG
server.port=${DEMO_PORT}
spring.datasource.url=jdbc:postgresql://localhost:5432/${DEMO_DB}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
app.jwt.secret=${JWT_SECRET}-DEMO
app.jwt.expire-minutes=480
app.cors-origin=*
ADMIN_USERNAME=${ADMIN_USERNAME}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
DEMOCFG

# ─── 5. Demo Seed Data ──────────────────────────────────────────────────
echo ""
echo "🌱 Writing demo seed SQL..."
cat > "${DEMO_DIR}/seed-demo.sql" << 'SEEDSQL'
-- Additional demo data (run after first startup seeds admin/locations)
-- Usage: PGPASSWORD=*** psql -U inventory_user -d inventory_demo -f /opt/inventory-manager/demo/seed-demo.sql

-- Extra branches
INSERT INTO branches (name, address, state_id, municipality_id, parish_id, created_at, updated_at)
SELECT 'Sede Central', 'Av. Principal, Edif. Logística, Piso 1', s.id, m.id, p.id, NOW(), NOW()
FROM states s, municipalities m, parishes p
WHERE s.name = 'Default State' AND m.name = 'Default Municipality' AND p.name = 'Default Parish'
AND NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Sede Central');

INSERT INTO branches (name, address, state_id, municipality_id, parish_id, created_at, updated_at)
SELECT 'Almacén Norte', 'Calle 5, Zona Industrial Norte', s.id, m.id, p.id, NOW(), NOW()
FROM states s, municipalities m, parishes p
WHERE s.name = 'Default State' AND m.name = 'Default Municipality' AND p.name = 'Default Parish'
AND NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Almacén Norte');

INSERT INTO branches (name, address, state_id, municipality_id, parish_id, created_at, updated_at)
SELECT 'Depósito Sur', 'Av. Libertador, Sector Sur', s.id, m.id, p.id, NOW(), NOW()
FROM states s, municipalities m, parishes p
WHERE s.name = 'Default State' AND m.name = 'Default Municipality' AND p.name = 'Default Parish'
AND NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Depósito Sur');

-- Categories
INSERT INTO categories (name, created_at, updated_at)
SELECT name, NOW(), NOW() FROM (VALUES
  ('Electrónicos'), ('Ferretería'), ('Oficina'), ('Limpieza'), ('Seguridad'), ('Emergencia')
) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE categories.name = t.name);

-- Items (referencing the correct branch's Storage department)
DO $$
DECLARE
  central_id bigint;
  norte_id bigint;
  sur_id bigint;
  storage_central bigint;
  storage_norte bigint;
  storage_sur bigint;
  cat_elec bigint; cat_ferr bigint; cat_ofi bigint; cat_limp bigint; cat_seg bigint;
BEGIN
  SELECT id INTO central_id FROM branches WHERE name = 'Sede Central';
  SELECT id INTO norte_id FROM branches WHERE name = 'Almacén Norte';
  SELECT id INTO sur_id FROM branches WHERE name = 'Depósito Sur';
  SELECT id INTO storage_central FROM departments WHERE name = 'Storage' AND branch_id = central_id;
  SELECT id INTO storage_norte FROM departments WHERE name = 'Storage' AND branch_id = norte_id;
  SELECT id INTO storage_sur FROM departments WHERE name = 'Storage' AND branch_id = sur_id;
  SELECT id INTO cat_elec FROM categories WHERE name = 'Electrónicos';
  SELECT id INTO cat_ferr FROM categories WHERE name = 'Ferretería';
  SELECT id INTO cat_ofi FROM categories WHERE name = 'Oficina';
  SELECT id INTO cat_limp FROM categories WHERE name = 'Limpieza';
  SELECT id INTO cat_seg FROM categories WHERE name = 'Seguridad';

  INSERT INTO items (name, quantity, unit, observations, branch_id, department_id, category_id, created_at, updated_at)
  VALUES
    ('Laptop HP ProBook 450', 15, 'UND', 'i5, 16GB RAM, SSD 256GB', central_id, storage_central, cat_elec, NOW(), NOW()),
    ('Monitor LG 24" Full HD', 30, 'UND', 'HDMI, VGA, incluye cable', central_id, storage_central, cat_elec, NOW(), NOW()),
    ('Teclado USB Genérico', 40, 'UND', 'Cableado, layout ES', central_id, storage_central, cat_elec, NOW(), NOW()),
    ('Mouse Óptico USB', 45, 'UND', '1600 DPI, cable 1.5m', central_id, storage_central, cat_elec, NOW(), NOW()),
    ('Taladro Percutor Bosch', 10, 'UND', '800W, con maletín + brocas', norte_id, storage_norte, cat_ferr, NOW(), NOW()),
    ('Caja Clavos 2" x1000', 200, 'UND', 'Acero templado', norte_id, storage_norte, cat_ferr, NOW(), NOW()),
    ('Martillo de Uña 16oz', 20, 'UND', 'Mango de fibra de vidrio', norte_id, storage_norte, cat_ferr, NOW(), NOW()),
    ('Resma Papel Carta 75g', 500, 'UND', 'Caja x 10 resmas, blanco', central_id, storage_central, cat_ofi, NOW(), NOW()),
    ('Tóner HP 26A Negro', 12, 'UND', 'Original, para LaserJet Pro', central_id, storage_central, cat_ofi, NOW(), NOW()),
    ('Desinfectante Lysol 5L', 60, 'L', 'Concentrado, aroma limón', sur_id, storage_sur, cat_limp, NOW(), NOW()),
    ('Jabón Líquido 1L', 80, 'L', 'Antibacterial, neutro', sur_id, storage_sur, cat_limp, NOW(), NOW()),
    ('Casco Seguridad Industrial', 25, 'UND', 'ANSI Z89.1, color blanco', sur_id, storage_sur, cat_seg, NOW(), NOW()),
    ('Chaleco Reflectivo', 30, 'UND', 'Talla única, naranja', sur_id, storage_sur, cat_seg, NOW(), NOW()),
    ('Extintor ABC 5kg', 8, 'UND', 'Polvo químico seco, con soporte', sur_id, storage_sur, cat_seg, NOW(), NOW())
  ON CONFLICT (name) DO NOTHING;

  -- Bags with expected items
  INSERT INTO bags (name, barcode, branch_id, assigned_department_id, created_at, updated_at)
  VALUES ('Kit Cómputo Central', 'DEMO-BAG-001', central_id, storage_central, NOW(), NOW())
  ON CONFLICT (barcode) DO NOTHING;
END $$;
SEEDSQL

# ─── 6. Systemd Services ──────────────────────────────────────────────
echo ""
echo "⚙️  Creating systemd services..."

cat > /etc/systemd/system/inventory-prod.service << 'PRODEOF'
[Unit]
Description=Inventory Manager — Production Backend
Documentation=https://github.com/GGabrielDev/inventory-manager-java
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=logistica
Group=logistica
WorkingDirectory=/opt/inventory-manager/prod
ExecStart=/usr/bin/java -jar /opt/inventory-manager/prod/backend-exec.jar --spring.config.location=/opt/inventory-manager/prod/application.properties
Restart=on-failure
RestartSec=10
StartLimitBurst=3
StartLimitInterval=60s

[Install]
WantedBy=multi-user.target
PRODEOF

cat > /etc/systemd/system/inventory-demo.service << 'DEMOEOF'
[Unit]
Description=Inventory Manager — Demo Backend (manual start)
Documentation=https://github.com/GGabrielDev/inventory-manager-java
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=logistica
Group=logistica
WorkingDirectory=/opt/inventory-manager/demo
ExecStart=/usr/bin/java -jar /opt/inventory-manager/demo/backend-exec.jar --spring.config.location=/opt/inventory-manager/demo/application.properties
Restart=no

[Install]
WantedBy=multi-user.target
DEMOEOF

systemctl daemon-reload

# ─── 7. Deploy JARs ───────────────────────────────────────────────────
echo ""
echo "📦 Deploy backend JAR..."
# If setup-server.sh is run from the project directory, it copies the JAR
if [ -f ./backend/target/backend-*-exec.jar ]; then
  JAR=$(ls ./backend/target/backend-*-exec.jar | head -1)
  cp "$JAR" "${PROD_DIR}/backend-exec.jar"
  cp "$JAR" "${DEMO_DIR}/backend-exec.jar"
  chown logistica:logistica "${PROD_DIR}/backend-exec.jar" "${DEMO_DIR}/backend-exec.jar"
  chmod 644 "${PROD_DIR}/backend-exec.jar" "${DEMO_DIR}/backend-exec.jar"
  echo "  ✓ JAR deployed to prod + demo"
else
  echo "  ⚠️  No JAR found in ./backend/target/"
  echo "  Build it with: mvn clean package -DskipTests"
  echo "  Then copy manually:"
  echo "    cp backend/target/backend-*-exec.jar ${PROD_DIR}/backend-exec.jar"
  echo "    cp backend/target/backend-*-exec.jar ${DEMO_DIR}/backend-exec.jar"
fi

# ─── 8. Seed Demo Database ────────────────────────────────────────────
echo ""
echo "  ✓ Demo data ready at ${DEMO_DIR}/seed-demo.sql"
echo "  Run after first startup to add showcase data:"
echo "    PGPASSWORD=${DB_PASS} psql -U ${DB_USER} -d ${DEMO_DB} -f ${DEMO_DIR}/seed-demo.sql"

# ─── 9. Start Services ────────────────────────────────────────────────
echo ""
echo "✅ Starting production service..."
systemctl enable inventory-prod
systemctl start inventory-prod
echo "  ✓ inventory-prod active (port ${PROD_PORT})"

echo ""
echo "  Demo service is DISABLED by default."
echo "    Start:   systemctl start inventory-demo"
echo "    Status:  systemctl status inventory-demo"
echo "    Logs:    journalctl -u inventory-demo --no-pager -n 50 -f"

# ─── 10. Save credentials for re-run ────────────────────────────────
mkdir -p "${APP_DIR}"
cat > "${CONFIG_FILE}" << CREDEOF
DB_PASS=${DB_PASS}
JWT_SECRET=${JWT_SECRET}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
CREDEOF
chmod 600 "${CONFIG_FILE}"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                   SAVE THESE CREDENTIALS!                   ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Database password:  ${DB_PASS}                        ║"
echo "║  Admin login:        ${ADMIN_USERNAME} / ${ADMIN_PASSWORD}              ║"
echo "║                                                              ║"
echo "║  Credentials also saved to: ${CONFIG_FILE}                ║"
echo "║  Re-running this script will reuse the same credentials.     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "================================================"
echo " ✅ Setup complete!"
echo "================================================"
echo ""
echo "  API endpoints:"
echo "    Production  http://192.168.1.21:${PROD_PORT}/api"
echo "    Demo        http://192.168.1.21:${DEMO_PORT}/api"
echo ""
echo "  Frontend: run inventory-manager-java desktop app"
echo "    or set:  INVENTORY_API_URL=http://192.168.1.21:${PROD_PORT}/api /path/to/frontend.jar"
echo ""
echo "  Admin login:"
echo "    ${ADMIN_USERNAME} / ${ADMIN_PASSWORD}"
echo ""
echo "  Demo credentials also saved to ${CONFIG_FILE}"
