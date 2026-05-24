#!/bin/bash
# ============================================================
# DondeAnime - Setup inicial de un VPS Ubuntu para producción.
# ------------------------------------------------------------
# Ejecutar como root la PRIMERA vez tras provisionar el VPS.
#
# Hace en orden:
#  1. Actualiza paquetes del sistema.
#  2. Instala Docker, Docker Compose, ufw, git.
#  3. Crea usuario 'deploy' (sin password, con sudo NOPASSWD).
#  4. Sube la SSH key pública de Diego al deploy.
#  5. Configura firewall: solo 22, 80, 443 (tcp+udp).
#  6. Endurece SSH: sin root login, sin password auth.
#
# Uso desde el Mac de Diego:
#   scp scripts/setup-vps.sh root@IP_VPS:/tmp/
#   ssh root@IP_VPS "bash /tmp/setup-vps.sh"
#
# Tras ejecutar:
#   - La sesión SSH como root se cae (porque deshabilita root login).
#   - Conectar como: ssh deploy@IP_VPS (sin password, con SSH key).
# ============================================================

set -euo pipefail

# SSH key pública de Diego (Mac). Si en el futuro otro dev necesita
# acceso, añadir otra key al final del authorized_keys.
SSH_PUB_KEY="ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGpGvdTfE+FdxJV9/hmRdna0YPNTk8lB4HH6fjv29beq diego@dondeanime"

echo "==> 1/6 Actualizando paquetes del sistema..."
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -qq

echo "==> 2/6 Instalando Docker, docker compose plugin, ufw, git..."
apt-get install -y -qq docker.io docker-compose-v2 ufw git ca-certificates
systemctl enable --now docker

echo "==> 3/6 Creando usuario 'deploy'..."
if id deploy &>/dev/null; then
    echo "    Usuario deploy ya existe, saltando creación."
else
    adduser --disabled-password --gecos "" deploy
    usermod -aG docker deploy
    usermod -aG sudo deploy
    # sudo sin password para que deploy pueda mantener el VPS
    # cómodamente sin tener que recordar credenciales.
    echo "deploy ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/deploy
    chmod 440 /etc/sudoers.d/deploy
fi

echo "==> 4/6 Instalando SSH key del Mac de Diego en deploy..."
mkdir -p /home/deploy/.ssh
echo "$SSH_PUB_KEY" > /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

echo "==> 5/6 Configurando firewall ufw..."
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP (redirect + Let'\''s Encrypt challenge)'
ufw allow 443/tcp comment 'HTTPS'
ufw allow 443/udp comment 'HTTP/3 (QUIC)'
ufw --force enable

echo "==> 6/6 Endureciendo SSH (sin root login, sin password auth)..."
sed -i 's/^#\?PermitRootLogin .*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config
sed -i 's/^#\?PubkeyAuthentication .*/PubkeyAuthentication yes/' /etc/ssh/sshd_config
systemctl restart ssh

echo ""
echo "================================================================"
echo "✓ Setup completado."
echo ""
echo "Acciones requeridas en el Mac de Diego:"
echo ""
echo "  1. Cerrar esta sesión SSH (la sesión root quedará rota porque"
echo "     hemos deshabilitado root login)."
echo ""
echo "  2. Reconectar como deploy (NO debe pedir password):"
echo "     ssh deploy@<IP_VPS>"
echo ""
echo "  3. Verificar Docker funciona:"
echo "     docker ps"
echo ""
echo "================================================================"
