$(document).ready(function() {
    $('#switch-as').change(function () {
        if (this.checked) {
            $('.adv-search').removeClass('hidden');
        } else {
            $('.adv-search').addClass('hidden');
        }
    });

    $('#switch-hqt').change(function () {
        if (this.checked) {
            $('.highlight').addClass('highlight-on');
        } else {
            $('.highlight-on').removeClass('highlight-on');
        }
    });

    $('#switch-spq').change(function () {
        if (this.checked) {
            $('#parsed-query').removeClass('hidden');
        } else {
            $('#parsed-query').addClass('hidden');
        }
    });

    $('#switch-sri').change(function () {
        if (this.checked) {
            $('#report-info').removeClass('hidden');
        } else {
            $('#report-info').addClass('hidden');
        }
    });

    $('form input[type=radio],textarea').each(function() {
        $(this).data('original', $(this).val());
    });

    $(window).bind('beforeunload', function(e) {
        var dirty = false;
        $('form input[type=radio],textarea').each(function() {
            if($(this).data('original') != $(this).val()) {
                dirty = true;
            }
        });

        if (dirty) {
            var message = "You have some unsaved changes.";
            e.returnValue = message;
            return message;
        }
    });

    if (!$("input[name='j']:checked").val()) {
        var submit = $('form button[type=submit]')
        submit.attr("disabled", "disabled");
        submit.addClass('mdl-button--disabled');
        $("input[name='j']").change(function() {
            submit.removeAttr("disabled")
            submit.removeClass('mdl-button--disabled');
        })
    }

    $('form').submit(function(event) {
        event.preventDefault();
        var form = $(this);

        $('#response-box').removeClass('hidden mdl-color--green-100 mdl-color--red-100')
            .addClass('mdl-color--yellow-100')
            .html('<b>Saving judgment...</b> <div class="mdl-spinner mdl-spinner--single-color mdl-js-spinner is-active"></div>')
            .show();

        $.ajax({
            type: "POST",
            url: 'save-judgment',
            data: form.serialize(),
        }).done(function(data) {
            $('#response-box').removeClass('mdl-color--yellow-100 mdl-color--red-100')
                .addClass('mdl-color--green-100')
                .html(data);
            $('form input[type=radio],textarea').each(function() {
                $(this).data('original', $(this).val());
            });
            $('#green-next').removeClass('hidden');

        }).fail(function(data) {
            $('#response-box').removeClass('mdl-color--yellow-100 mdl-color--green-100')
                .addClass('mdl-color--red-100')
                .html(data);
        });
    });
});