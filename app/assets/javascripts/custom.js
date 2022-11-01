// $.fn.apiSubscriber = function () {
//     var self = this;

//     var showFields = function (event) {
//         var $clickedToggle = $(event.target),
//             fieldsContainer = "#" + $clickedToggle.attr("data-fields-container-id")
//         showElement($(fieldsContainer)) // Show the relative fields container div so that a user can populate custom fields
//     };

//     var showError = function() {
//         $(self).find(".error-notification").css("display", "block");
//         $(self).find(".submit-button").prop('disabled', false);
//     };

//     var subscribe = function (event) { // subscribes user to api posting a serliazed form
//         var $form = $(this),
//             url = $form.attr("action");

//         event.preventDefault();
//         $.post(url, $form.serialize(), function (response, textStatus, xhr) {
//             subscribeResponseHandler(response, xhr)
//         })
//         .fail(function () {
//             showError()
//         });
//     };

//     var subscribeResponseHandler = function(response, xhr) {
//         var updateSubscription = function() {
//             $("#subscriptions-" + encodeString(response.apiName) + "-" + encodeString(response.group)).text(response.numberOfSubscriptionText);
//             showElement($(self).find(".toggle.subscribed"));
//             hideElement($(self).find(".toggle.not-subscribed"));
//             $(self).find(".error-notification").css("display", "none");
//             $(self).find(".submit-button").prop('disabled', false);
//         };

//         function isSuccess() {
//             return /2[0-9]{0,2}/g.test(xhr.status);
//         }

//         if (isSuccess()) {
//             updateSubscription(response);
//         } else {
//             showError();
//         }
//     };

//     var encodeString = function(s){
//         return s.replace(/[^0-9A-Z]+/gi, '_')
//     };

//     var hideElement = function (element) {
//         element.addClass('hidden');
//     };

//     var showElement = function (element) {
//         element.removeClass('hidden');
//     };

//     $(self).find(".has_fields").on("click", showFields);
//     $(self).find(".fields-subscription, .no-fields-subscription").on("submit", subscribe);
// };

// $(document).ready(function(){
//     $(".conditionallyHide").each(function() {
//         if ($(this).attr("data-hide")) $(this).addClass("hidden");
//     });

//     $(".api-subscriber").each(function(){
//         $(this).apiSubscriber()});
// });
