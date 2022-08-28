// this javascript is either
// - embedded inside the SVG in an SVG <script> tag
// - embedded inside the HTML in an HTML <script> tag
//
// the initSqlExplain() function is called from an 'onload' attribute on the SVG element

// the tooltip code has been adapted from the javascript at
//   https://github.com/petercollingridge/code-for-blog/blob/master/svg-interaction/tooltip/change_text_dynamic_length.svg?short_path=b59eb3f

function initSqlExplain(e) {
    svg = e.target.ownerDocument;
    svgRoot = e.target; // svg.documentElement

    var tooltip = svg.getElementById('tooltip');
    var tooltipFo = tooltip.getElementsByTagName('foreignObject')[0];
    var tooltipDiv = tooltipFo.getElementsByTagName('div')[0];
    var triggers = svg.querySelectorAll('[data-tooltip-html]');

    for (var i = 0; i < triggers.length; i++) {
        triggers[i].addEventListener('mousemove', showTooltip);
        triggers[i].addEventListener('mouseout', hideTooltip);
    }

    function showTooltip(evt) {
        var CTM = svgRoot.getScreenCTM();
        var x = (evt.clientX - CTM.e + 6) / CTM.a;
        var y = (evt.clientY - CTM.f + 20) / CTM.d;
        tooltip.setAttributeNS(null, "transform", "translate(" + x + " " + y + ")");
        tooltip.setAttributeNS(null, "visibility", "visible");

        tooltipDiv.innerHTML = evt.target.getAttributeNS(null, "data-tooltip-html");
    }

    function hideTooltip(evt) {
        tooltip.setAttributeNS(null, "visibility", "hidden");
    }
}
