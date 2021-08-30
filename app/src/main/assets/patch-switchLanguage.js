if (!$('#lang_switch').length) {
    $('html > head').append($('<style>#lang_switch.switched svg {transform:scaleY(-1)}</style>'));
    $(Android.getAssetsText('switchLanguage.html')).click(function(){
        this.classList.contains("switched") ? this.classList.remove("switched") : this.classList.add("switched");
        window.location = window.location.href.split('#')[0] +
            '#' + document.getElementsByClassName('lmt__language_select lmt__language_select--target')[0].getAttribute('dl-selected-lang').split('-')[0] +
            '/' + document.getElementsByClassName('lmt__language_select lmt__language_select--source')[0].getAttribute('dl-selected-lang').split('-')[0] +
            '/' + encodeURI(document.getElementsByClassName('lmt__textarea lmt__target_textarea lmt__textarea_base_style')[0].value);
    }).prependTo($('.lmt__language_container')[1]);
}