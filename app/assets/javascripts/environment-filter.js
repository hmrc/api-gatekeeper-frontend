$(document).ready(function () {

    var environmentFilter = $("#environmentFilter");

    function disable() {
        environmentFilter.attr("disabled", true)
    }

    function enable() {
        environmentFilter.attr("disabled", false)
    }

    var checkAndSetEnvironmentFilter = function () {
        if ("" === $(this).find(":selected").text()) {
            disable();
        } else {
            enable();
        }
    };

    $('.environment-filter-disable').on("change", checkAndSetEnvironmentFilter);

    $('.environment-filter-disable').each(checkAndSetEnvironmentFilter);
});
