$(document).ready(function() {

    // =====================================================
    // Initialise show-hide-content
    // Toggles additional content based on radio/checkbox input state
    // =====================================================
    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()

    // =====================================================
    // Handle number inputs
    // =====================================================
    numberInputs();

    // =====================================================
    // Back link mimics browser back functionality
    // =====================================================
    $('#back-link').on('click', function(e){
        e.preventDefault();
        window.history.back();
    })


    if(document.querySelectorAll('select').length > 0){
        openregisterLocationPicker({
            defaultValue: '',
            selectElement: document.querySelector('select'),
            url: '/manage-pension-schemes/assets/javascripts/autocomplete/location-autocomplete-graph.json'
        })

        // temporary fix for IE not registering clicks on the text of the results list for the country autocomplete
        $('body').on('mouseup', ".autocomplete__option > strong", function(e){
            e.preventDefault(); $(this).parent().trigger('click')
        })

        // temporary fix for the autocomplete holding onto the last matching country when a user then enters an invalid or blank country
        $('input[role="combobox"]').on('keydown', function(e){
            if (e.which != 13 && e.which != 9) {
                var sel = document.querySelector('.autocomplete-wrapper select')
                sel.value = "";
            }
        })
    }

    function beforePrintCall(){
        if($('.no-details').length > 0){
            // store current focussed element to return focus to later
            var fe = document.activeElement;
            // store scroll position
            var scrollPos = window.pageYOffset;
            $('details').not('.open').each(function(){
                $(this).addClass('print--open');
                $(this).find('summary').trigger('click');
            });
            // blur focus off current element in case original cannot take focus back
            $(document.activeElement).blur();
            // return focus if possible
            $(fe).focus();
            // return to scroll pos
            window.scrollTo(0,scrollPos);
        } else {
            $('details').attr("open","open").addClass('print--open');
        }
        $('details.print--open').find('summary').addClass('heading-medium');
    }

    function afterPrintCall(){
        $('details.print--open').find('summary').removeClass('heading-medium');
        if($('.no-details').length > 0){
            // store current focussed element to return focus to later
            var fe = document.activeElement;
            // store scroll position
            var scrollPos = window.pageYOffset;
            $('details.print--open').each(function(){
                $(this).removeClass('print--open');
                $(this).find('summary').trigger('click');
            });
            // blur focus off current element in case original cannot take focus back
            $(document.activeElement).blur();
            // return focus if possible
            $(fe).focus();
            // return to scroll pos
            window.scrollTo(0,scrollPos);
        } else {
            $('details.print--open').removeAttr("open").removeClass('print--open');
        }
    }

    //Chrome
    if(typeof window.matchMedia != 'undefined'){
        mediaQueryList = window.matchMedia('print');
        mediaQueryList.addListener(function(mql) {
            if (mql.matches) {
                beforePrintCall();
            };
            if (!mql.matches) {
                afterPrintCall();
            };
        });
    }

    //Firefox and IE (above does not work)
    window.onbeforeprint = function(){
        beforePrintCall();
    }
    window.onafterprint = function(){
        afterPrintCall();
    }
});


function numberInputs() {
    // =====================================================
    // Set currency fields to number inputs on touch devices
    // this ensures on-screen keyboards display the correct style
    // don't do this for FF as it has issues with trailing zeroes
    // =====================================================
    if($('html.touchevents').length > 0 && window.navigator.userAgent.indexOf("Firefox") == -1){
        $('[data-type="currency"] > input[type="text"], [data-type="percentage"] > input[type="text"]').each(function(){
            $(this).attr('type', 'number');
            $(this).attr('step', 'any');
            $(this).attr('min', '0');
        });
    }

    // =====================================================
    // Disable mouse wheel and arrow keys (38,40) for number inputs to prevent mis-entry
    // also disable commas (188) as they will silently invalidate entry on Safari 10.0.3 and IE11
    // =====================================================
    $("form").on("focus", "input[type=number]", function(e) {
        $(this).on('wheel', function(e) {
            e.preventDefault();
        });
    });
    $("form").on("blur", "input[type=number]", function(e) {
        $(this).off('wheel');
    });
    $("form").on("keydown", "input[type=number]", function(e) {
        if ( e.which == 38 || e.which == 40 || e.which == 188 )
            e.preventDefault();
    });
}
