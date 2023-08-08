function apiApprovalConfirmation(clickedElement) {
  document.getElementById('submit').disabled = !clickedElement.checked
}
  ​
function initApiApprovalConfirmationOnChange(clickedElementId) {
  if (clickedElementId != null) {
    var clickedElement = document.getElementById(clickedElementId)
    if (clickedElement != null) {
      clickedElement.addEventListener(
        'change',
        function () {
          apiApprovalConfirmation(clickedElement)
        },
        false
      )
    }
  }
}