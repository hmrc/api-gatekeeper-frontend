$(document).ready(function () {

    var filters = $('.environment-filter-disable');

    filters.each(function (index) {

        var apiVersionFilter = this
        var environmentFilter = document.getElementById("environmentFilter");

        function disable() {
            environmentFilter.disabled = true;
        }

        function enable() {
            environmentFilter.disabled = false;
        }

        var checkAndSetEnvironmentFilter = function () {
            if ("" === apiVersionFilter.options[apiVersionFilter.selectedIndex].value) {
                disable();
            } else {
                enable();
            }
        }

        apiVersionFilter.addEventListener("change", checkAndSetEnvironmentFilter);

        checkAndSetEnvironmentFilter();
    })
});
