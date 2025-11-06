#!/bin/bash

# ===================================================
# AWS EC2 Ubuntu 환경에 맞게 수정된 서버 모니터링 스크립트
#
# 필요 패키지:
# 1. sysstat (iostat, sar 명령어 사용): sudo apt install sysstat
# 2. nvme-cli (NVMe SSD SMART 정보 확인용): sudo apt install nvme-cli
# ===================================================

# --- 헬퍼 함수: 명령어 존재 여부 확인 ---
check_dependency() {
    if ! command -v "$1" &> /dev/null; then
        echo "Error: Command '$1' not found. Please install the necessary package (e.g., sudo apt install sysstat or nvme-cli)." >&2
        exit 1
    fi
}

# --- 의존성 검사 ---
check_dependency "top"
check_dependency "free"
check_dependency "iostat"
check_dependency "sar"

# ===================================================
# 1CPU 사용률 확인
# ===================================================

# CPU 사용률 (IDLE 값으로 부터 USAGE 계산)
CPU_IDLE=$(top -bn1 | grep "Cpu(s)" | awk '{printf("%d", $8)}')
CPU_USAGE=$((100 - CPU_IDLE))

# ===================================================
# 메모리 사용량 확인 및 계산
# ===================================================
MEM_TOTAL=$(free -m | awk 'NR==2{print $2}')
MEM_AVAILABLE=$(free -m | awk 'NR==2{print $7}')
MEM_REAL_USED=$((MEM_TOTAL - MEM_AVAILABLE))
# bc를 사용하여 소수점 계산
MEM_PERCENT=$(echo "scale=1; (${MEM_TOTAL} - ${MEM_AVAILABLE}) * 100 / ${MEM_TOTAL}" | bc)

# ===================================================
# 네트워크 트래픽 (MB/s) 및 디스크 I/O (MB/s) 확인
# ===================================================

# A. 디스크 I/O (iostat 사용) - AWS 인스턴스 스토어 디바이스명 nvme0n1 사용
# 주의: EBS Root Volume은 /dev/xvda 또는 /dev/sda 일 수 있습니다. 필요 시 변경하세요.
DISK_DEVICE="sda1"
IOSTAT_DATA=$(iostat -d 1 2 | grep "$DISK_DEVICE" | tail -n 1)

# rkB/s (read)와 wkB/s (write) 값을 추출 (필드 위치는 iostat 버전에 따라 다를 수 있으나, 보통 5번째와 6번째)
DISK_READ_KB=$(echo "$IOSTAT_DATA" | awk '{print $5}')
DISK_WRITE_KB=$(echo "$IOSTAT_DATA" | awk '{print $6}')

# B. 네트워크 트래픽 (sar -n DEV 사용: eth0 인터페이스)
# 주의: 최신 AWS AMI에서는 인터페이스 이름이 ens5, enpXsX 등으로 다를 수 있습니다.
NET_INTERFACE="enX0"
NET_DATA=$(sar -n DEV 1 2 | grep "$NET_INTERFACE" | tail -n 1)

# rxkB/s (Download)와 txkB/s (Upload) 값을 추출
NET_RX_KB=$(echo "$NET_DATA" | awk '{print $5}')
NET_TX_KB=$(echo "$NET_DATA" | awk '{print $6}')

# C. MB/s 계산 후 소수점 2자리 포맷 강제 적용
DISK_READ_MB=$(printf "%.2f" $(echo "scale=2; ${DISK_READ_KB:-0} / 1024" | bc))
DISK_WRITE_MB=$(printf "%.2f" $(echo "scale=2; ${DISK_WRITE_KB:-0} / 1024" | bc))

NET_DOWNLOAD_MB=$(printf "%.2f" $(echo "scale=2; ${NET_RX_KB:-0} / 1024" | bc))
NET_UPLOAD_MB=$(printf "%.2f" $(echo "scale=2; ${NET_TX_KB:-0} / 1024" | bc))

# ===================================================
# 5. 결과 출력
# ===================================================
echo "=========================================="
echo "    AWS EC2 Ubuntu Instance Monitoring    "
echo "=========================================="
echo "CPU Usage: ${CPU_USAGE}%"
echo "------------------------------------------"
echo "Memory Usage: ${MEM_REAL_USED}MB / ${MEM_TOTAL}MB (${MEM_PERCENT}%)"
echo "------------------------------------------"
echo "Network  (MB/s): Down: ${NET_DOWNLOAD_MB} | Upload: ${NET_UPLOAD_MB}"
echo "Disk I/O (MB/s): Read: ${DISK_READ_MB} | Write : ${DISK_WRITE_MB}"
echo "=========================================="