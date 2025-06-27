// Lấy các phần tử
const refundType = document.getElementById('refundType');
const refundAmount = document.getElementById('refundAmount');

function isValidForm() {
    const amount = document.getElementById('amount').value;
    const id = document.getElementById('paymentId').value;
    const createBy = document.getElementById('createBy').value;

    let isValid = true;

    if (!id || id < 1 ) {
        document.getElementById('idError').textContent = 'Mã giao dịch không được để trống và phải lớn hơn 0.';
        isValid = false;
    } else {
        document.getElementById('idError').textContent = '';
    }

    if (refundType.value === '03') {
        if (!amount || amount <= 0) {
                document.getElementById('amountError').textContent = 'Số tiền không được để trống.';
                isValid = false;
        } else if (amount < 5000) {
                document.getElementById('amountError').textContent = 'Số tiền tối thiểu là 5,000 VND.';
                isValid = false;
        } else if (amount > 1000000000) {
                document.getElementById('amountError').textContent = 'Số tiền tối đa là 1,000,000,000 VND.';
                isValid = false;
        } else {
                document.getElementById('amountError').textContent = '';
        }
    }

    if (!createBy || createBy.trim() === '') {
        document.getElementById('createByError').textContent = 'Không được để trống';
        isValid = false;
    } else {
        document.getElementById('createByError').textContent = '';
    }

    return isValid
}

// Hàm để cập nhật hiển thị
function updateAmountVisibility() {
    if (refundType.value === '03') { // Hoàn trả một phần
        refundAmount.style.display = 'block';
    } else { // Hoàn trả toàn phần
        refundAmount.style.display = 'none';
    }
}

// Gắn sự kiện change
refundType.addEventListener('change', updateAmountVisibility);

// Kích hoạt khi tải trang
window.addEventListener('load', updateAmountVisibility);

document.getElementById('refundForm').addEventListener('submit', function(event) {
      let isValid = isValidForm();
      if (!isValid) {
        event.preventDefault();
      }
});