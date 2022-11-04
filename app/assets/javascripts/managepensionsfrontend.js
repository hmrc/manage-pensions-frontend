GOVUKFrontend.initAll();
HMRCFrontend.initAll();

if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

    var backLink = document.querySelector('.govuk-back-link');
    if (backLink) {
        backLink.addEventListener('click', function (e) {
            e.preventDefault();
            if (window.history && window.history.back && typeof window.history.back === 'function') {
                window.history.back();
            }
        });
    }
    
    var printLink = document.querySelector('#print-this-page-link');
    if (printLink) {
        printLink.addEventListener('click', function (e) {
            window.print();
            return false;
        });
    }

    var selectEl = document.querySelector('#country')
    if (selectEl !== null){
        accessibleAutocomplete.enhanceSelectElement({
            defaultValue: "",
            selectElement: selectEl
        })

        document.querySelector('input[role="combobox"]').addEventListener('keydown', function(e){
            if (e.which != 13 && e.which != 9) {
                selectEl.value = "";
            }
        });
    }

});
