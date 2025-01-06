function applicationsPageSize(clickedElement) {
  clickedElement.form.submit()
}

function initPageSizeOnChange(clickedElementId) {
  if (clickedElementId != null) {
    var clickedElement = document.getElementById(clickedElementId)
    if (clickedElement != null) {
      clickedElement.addEventListener(
        'change',
        function () {
          applicationsPageSize(clickedElement)
        },
        false
      )
    }
  }
}