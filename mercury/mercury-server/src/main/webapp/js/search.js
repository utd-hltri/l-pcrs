/**
 * Created by ramon on 6/6/17.
 */
$(document).ready(function() {
    $('#switch-as').change(function () {
        if (this.checked) {
            $('.adv-search').removeClass('hidden');
        } else {
            $('.adv-search').addClass('hidden');
        }
    });
});