function isValidForm() {
    const id = document.getElementById('queryId').value;

    let isValid = true;

    if (!id || id <= 0) {
            document.getElementById('idError').textContent = 'Mã GD không được để trống và phải lớn hn 0.';
            isValid = false;
    } else {
            document.getElementById('idError').textContent = '';
    }

    return isValid
}

document.getElementById('queryForm').addEventListener('submit', function(event) {
      let isValid = isValidForm();
      if (!isValid) {
        event.preventDefault();
      }
});