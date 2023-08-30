function copyText(id) { 
  var elem = document.getElementById(id);
  navigator.clipboard.writeText(elem.textContent);
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