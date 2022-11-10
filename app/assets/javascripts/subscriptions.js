(function() {
  function subscribe(event) {
    const offButton = event.target.id.substr(0, event.target.id.length-2) + 'off';
    document.getElementById(event.target.id).checked = true;
    document.getElementById(offButton).checked = false;
    document.getElementById(offButton).removeAttribute('checked');

    const subscribeForm = event.target.parentElement;
    subscribeForm.submit();
  }

  function unsubscribe(event) {
    const onButton = event.target.id.substr(0, event.target.id.length-3) + 'on';
    document.getElementById(event.target.id).checked = true;
    document.getElementById(onButton).checked = false;
    document.getElementById(onButton).removeAttribute('checked');

    const unsubscribeForm = event.target.parentElement;
    unsubscribeForm.submit();
  }
  
  document.querySelectorAll(".slider__on-submit").forEach(function(item, index, arr) {
    item.addEventListener("click", subscribe);
  });

  document.querySelectorAll(".slider__off-submit").forEach(function(item, index, arr) {
    item.addEventListener("click", unsubscribe);
  });
})();