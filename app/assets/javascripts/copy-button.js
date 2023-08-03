function copyText(id) {
  var copyText = document.getElementById(id);
  var textArea = document.createElement("textarea");
  textArea.value = copyText.textContent;
  document.body.appendChild(textArea);
  textArea.select();
  document.execCommand("Copy");
  textArea.remove();
}

function initCopyTextOnClick(clickedElementId, textSourceId) {
  if (clickedElementId != null) {
    var clickedElement = document.getElementById(clickedElementId)
    if (clickedElement != null) {
      clickedElement.addEventListener(
        'click',
        function () {
          copyText(textSourceId)
        },
        false
      )
    }
  }
}