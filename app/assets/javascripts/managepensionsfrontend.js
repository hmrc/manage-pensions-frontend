// initialise GovUK lib
GOVUKFrontend.initAll();
HMRCFrontend.initAll();

// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

    var backLink = document.querySelector('.govuk-back-link');
    if (backLink) {
        // backLink.classList.remove('js-visible');
        backLink.addEventListener('click', function (e) {
            e.preventDefault();
            if (window.history && window.history.back && typeof window.history.back === 'function') {
                window.history.back();
            }
        });
    }

    var printLink = document.querySelector('.print-this-page');
    if (printLink) {
        printLink.addEventListener('click', function (e) {
            window.print();
            return false;
        });
    }

    // handle country picker
    var selectEl = document.querySelector('#country')
    if( selectEl !== null ){
        accessibleAutocomplete.enhanceSelectElement({
            defaultValue: "",
            selectElement: selectEl
        })

        // fix to ensure error when blank
        document.querySelector('input[role="combobox"]').addEventListener('keydown', function(e){
            if (e.which != 13 && e.which != 9) {
                selectEl.value = "";
            }
        });
    }
});
