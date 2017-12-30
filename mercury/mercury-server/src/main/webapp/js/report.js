$(document).ready(function() {
    $('#switch-sa').change(function () {
        if (this.checked) {
            $('.concept').addClass('concept-on');
            $('.activity').addClass('activity-on');
            $('.eeg_event').addClass('event-on');
            $('.problem').addClass('problem-on');
            $('.test').addClass('test-on');
            $('.treatment').addClass('treatment-on');
            $('.attr').removeClass('hidden');
            $('.conc-key').removeClass('hidden');
        } else {
            $('.concept').removeClass('concept-on');
            $('.activity').removeClass('activity-on');
            $('.eeg_event').removeClass('event-on');
            $('.problem').removeClass('problem-on');
            $('.test').removeClass('test-on');
            $('.treatment').removeClass('treatment-on');
            $('.attr').addClass('hidden');
            $('.conc-key').addClass('hidden');
        }
    });
});