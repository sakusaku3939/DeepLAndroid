var copyButton = $('.Target--Dwohg').eq(2);
copyButton.off('click');
copyButton.on('click', function () {
    const text = $('[class^="TextInput-module--textarea--"]').last().val();
    Android.copyClipboard(text);
});