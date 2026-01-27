/**
 * 대시보드 JavaScript (가벼운 버전)
 * 
 * 아키텍처 문서의 클라이언트 레이어 구조를 참고하되,
 * 최소한의 JavaScript만 사용하여 가볍게 구현
 */

// 실시간 업데이트 (선택적, 향후 WebSocket 연동 시 사용)
class DashboardUpdater {
    constructor(accountNo) {
        this.accountNo = accountNo;
        this.updateInterval = null;
    }
    
    // 주기적 업데이트 (30초마다)
    startAutoUpdate(interval = 30000) {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
        
        this.updateInterval = setInterval(() => {
            this.updateBalance();
        }, interval);
    }
    
    stopAutoUpdate() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }
    
    // 잔고 업데이트
    async updateBalance() {
        if (!this.accountNo) return;
        
        try {
            const response = await fetch(`/api/v1/accounts/${this.accountNo}/balance`);
            if (response.ok) {
                const balance = await response.json();
                this.updateBalanceUI(balance);
            }
        } catch (error) {
            console.error('잔고 업데이트 실패:', error);
        }
    }
    
    // UI 업데이트
    updateBalanceUI(balance) {
        const totalBalanceEl = document.querySelector('[data-balance="total"]');
        const availableBalanceEl = document.querySelector('[data-balance="available"]');
        const investedAmountEl = document.querySelector('[data-balance="invested"]');
        
        if (totalBalanceEl) {
            totalBalanceEl.textContent = this.formatCurrency(balance.totalBalance, balance.currency);
        }
        if (availableBalanceEl) {
            availableBalanceEl.textContent = this.formatCurrency(balance.availableBalance, balance.currency);
        }
        if (investedAmountEl) {
            investedAmountEl.textContent = this.formatCurrency(balance.investedAmount, balance.currency);
        }
    }
    
    // 통화 포맷팅
    formatCurrency(amount, currency) {
        return new Intl.NumberFormat('ko-KR', {
            style: 'decimal',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(amount) + ' ' + currency;
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    const accountNo = new URLSearchParams(window.location.search).get('accountNo');
    
    // 실시간 업데이트 활성화 (선택적)
    // const updater = new DashboardUpdater(accountNo);
    // updater.startAutoUpdate();
    
    // 테이블 정렬 (선택적)
    initTableSorting();
});

// 테이블 정렬 기능 (가벼운 버전)
function initTableSorting() {
    const tables = document.querySelectorAll('table');
    tables.forEach(table => {
        const headers = table.querySelectorAll('th');
        headers.forEach((header, index) => {
            header.style.cursor = 'pointer';
            header.addEventListener('click', () => {
                sortTable(table, index);
            });
        });
    });
}

function sortTable(table, columnIndex) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    const isAscending = table.dataset.sortDirection !== 'asc';
    table.dataset.sortDirection = isAscending ? 'asc' : 'desc';
    
    rows.sort((a, b) => {
        const aText = a.cells[columnIndex].textContent.trim();
        const bText = b.cells[columnIndex].textContent.trim();
        
        // 숫자 비교
        const aNum = parseFloat(aText.replace(/[^\d.-]/g, ''));
        const bNum = parseFloat(bText.replace(/[^\d.-]/g, ''));
        
        if (!isNaN(aNum) && !isNaN(bNum)) {
            return isAscending ? aNum - bNum : bNum - aNum;
        }
        
        // 문자열 비교
        return isAscending 
            ? aText.localeCompare(bText)
            : bText.localeCompare(aText);
    });
    
    rows.forEach(row => tbody.appendChild(row));
}
