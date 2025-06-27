function isValidForm() {
    const amount = document.getElementById('amount').value;
    const orderInfo = document.getElementById('orderInfo').value;

    let isValid = true;

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

    if (!orderInfo || orderInfo.trim() === '') {
            document.getElementById('orderInfoError').textContent = 'Nội dung thanh toán không được để trống.';
            isValid = false;
    } else if (orderInfo.length > 255) {
            document.getElementById('orderInfoError').textContent = 'Nội dung thanh toán tối đa 255 ký tự.';
            isValid = false;
    } else {
            document.getElementById('orderInfoError').textContent = '';
    }

    return isValid
}

document.getElementById('paymentForm').addEventListener('submit', function(event) {
      let isValid = isValidForm();
      if (!isValid) {
        event.preventDefault();
      }
});