
let lang = window.location.pathname.substring(1, 3);

function escapeHTML(str) {
    return new Option(str).innerHTML;
}

// open navigation sidebar
function openNav() {
  localStorage.setItem("sidebar", "open");
  // document.getElementById("mySidebar").style.width = "200px";
  // document.getElementById("mySidebar").style.overflowY = "auto"; // show scrollbar
  document.getElementById("mySidebar").style.left = "0px";
  document.getElementById("main").style.marginLeft = "200px";
}

// close navigation sidebar
function closeNav() {
  localStorage.setItem("sidebar", "closed");
  // document.getElementById("mySidebar").style.overflowY = "hidden"; // hide scrollbar
  // document.getElementById("mySidebar").style.width = "0px";
  document.getElementById("mySidebar").style.left = "-240px";
  document.getElementById("main").style.marginLeft= "30px";
}

let miniSearch = null;
let suggest = null;
let curSuggest = -1;
let origSuggest = null;

function doSuggest(i, search) {
    if (!suggest || !miniSearch)
        return false;

    if (i < 0)
        i = -1;
    else if (i >= suggest.length)
        i = suggest.length - 1;

    if (i == curSuggest)
        return false;

    let q = document.getElementById("query");

    // deactivate prevous selection
    if (curSuggest < 0) {
        origSuggest = q.value;
    } else {
        let si = document.getElementById("suggestion_" + curSuggest);
        if (si)
            si.className = 'suggestion';
    }

    // activate new selection
    curSuggest = i;
    if (i < 0) {
        q.value = origSuggest;
    } else {
        q.value = suggest[curSuggest].suggestion;
        let si = document.getElementById("suggestion_" + curSuggest);
        if (si)
            si.className = 'activesuggestion';
    }
    if (search)
        doSearch(null);

    return false;
}

function clearSuggest() {
    let ss = document.getElementById("suggest");
    if (ss)
        ss.remove();
    curSuggest = -1;
    suggest = null;
    origSuggest = null;
    localStorage.removeItem("suggest");
}

function keydown(event) {
    if (!suggest || !miniSearch)
        return false;

    if (event.key === 'ArrowDown') {
        event.preventDefault();
        doSuggest(curSuggest + 1, false);
    } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        doSuggest(curSuggest - 1, false);
    } else if (event.key === 'Escape') {
        event.preventDefault();
        clearSuggest();
    }
    return false;
}

function hideSearch() {
    clearSuggest();
    localStorage.removeItem("query");
    let sr = document.getElementById("results");
    if (sr) {
        sr.style.visibility = "hidden";
        sr.innerHTML = "";
    }
}

function doSearch(event) {
    if (event)
        event.preventDefault();
    clearSuggest();
    showSearch();
    return false;
}

function showSearch() {
    let q = document.getElementById("query");
    if (!q || !miniSearch)
        return;
    let query = q.value;
    localStorage.setItem("query", query);
    var results = miniSearch.search(query);
    var n = results.length;
    var sr = document.getElementById("results");
    sr.innerHTML = 'Found ' + n + ' results.<div id="closesearch"><a href="javascript:void(0)" title="Hide Search Results" onclick="hideSearch()">&#10006;</a></div>';
    for (var i = 0; i < n; i++) {
        sr.innerHTML += '<br>('+(i+1)+') <a href="' + results[i].url + '">' + results[i].title + '</a>';
    }
    sr.style.visibility = "visible";
}
    

function doIncremental(event) {
    var q = document.getElementById("query");
    if (!q || !miniSearch)
        return;
    var query = q.value;
    suggest = miniSearch.autoSuggest(query, {
                prefix: term => term.length > 2,
                fuzzy: term => term.length > 3 ? 0.2 : null
            });
    curSuggest = -1;
    origSuggest = query;
    var ss = document.getElementById("suggest");
    if (!ss) {
        ss = document.createElement("div");
        ss.id = "suggest";
        q.parentNode.appendChild(ss); // insertBefore(ss, query.nextSibling)
    }
    // get rid of some silly suggestions, and truncate to top 5 suggestions
    for (var i = 0; i < 5 && i < suggest.length; i++) {
        if (suggest[i].suggestion === query) {
            suggest.splice(i, 1);
            i--;
        }
    }
    if (suggest.length > 5)
        suggest = suggest.slice(0, 5);
    if (suggest.length > 0) {
        localStorage.setItem("suggest", "open");
        var suggestions = "";
        for (var i = 0; i < suggest.length; i++) {
            /* if (i > 0)
                    suggestions += "<br>"; */
            suggestions += '<span class="suggestion" id="suggestion_'+i+'" onclick="doSuggest('+i+', true)">' + suggest[i].score + " " + escapeHTML(suggest[i].suggestion) + '</span>';
        }
        ss.innerHTML = suggestions;
        ss.style.visibility = "visible";
    } else {
        clearSuggest();
    }

    showSearch();
    return false;
}

window.onload = function() {

    function loadStyle(url) {
        return new Promise((resolve, reject) => {
            let link = document.createElement('link');
            link.type = 'text/css';
            link.rel = 'stylesheet';
            link.onload = () => { resolve(); }
            link.href = url;
            let headScript = document.querySelector('script');
            headScript.parentNode.append(link);
        });
    }

    function loadScript(url, callback) {
        let head = document.head;
        let script = document.createElement('script');
        script.type = 'text/javascript';
        script.src = url;
        script.onreadystatechange = callback;
        script.onload = callback;
        head.appendChild(script);
    }

    // fetch sidebar data and inject sidebar into body
    async function loadSidebar() {
        const response = await fetch("/sidebar_"+lang+".html");
        const sidebar_list = await response.text();
        const body = document.body.innerHTML;
        document.body.innerHTML = 
          '<div id="myCollapsedBar" class="collapsedbar">'
        + '<a href="javascript:void(0)" title="Open Menu" class="menu" onclick="openNav()"></a>'
        + '</div>'
        + '<div id="mySidebar" class="sidebar">'
        + '<a href="javascript:void(0)" id="closebtn" class="btn" onclick="closeNav()">&lt</a>'
        + sidebar_list
        + '</div>'
        + '<div id="search"></div>'
        + '<div id="main">' + body + ' </div>';

        let fs = localStorage.getItem("sidebar");
        if (fs === "closed")
          closeNav()

        let sidebar = document.getElementById("mySidebar"); // document.querySelector(".sidebar");
        let top = localStorage.getItem("sidebar-scroll");
        if (top)
            sidebar.scrollTop = parseInt(top, 10);

        window.addEventListener("beforeunload", () => {
          localStorage.setItem("sidebar-scroll", sidebar.scrollTop);
        });
    }

    function restoreSearchState() {
        let searchQuery = localStorage.getItem("query");
        if (!searchQuery)
            return;
        let q = document.getElementById("query");
        q.value = searchQuery;

        let suggestState = localStorage.getItem("suggest");
        if (suggestState === "open") {
            doIncremental(null);
        } else {
            showSearch();
        }
    }

    async function fetchSearchInfo() {
        const response = await fetch("/contents_"+lang+".json");
        const documents = await response.json();

        miniSearch.addAll(documents);

        let search = document.getElementById("search");
        if (!search)
            return;
        search.innerHTML = '<form autocomplete="off" onsubmit="doSearch(event);">'
            + '<input type="submit" value="Search">'
            + '<span>'
            + '<input type="text" id="query" name="query" onkeydown="keydown(event)" oninput="doIncremental(event)"/>'
            + '</span>'
            + '<div id="results"></div>'
            + '</form>';
        restoreSearchState();
    }

    function setupSearch() {
        miniSearch = new MiniSearch({
            fields: ['title', 'text'], // fields to index for full-text search
            storeFields: ['title', 'url', ], // fields to return with search results
            searchOptions: {
                prefix: term => term.length > 2,
                fuzzy: term => term.length > 3 ? 0.2 : null
            }
        });
        fetchSearchInfo();
    }

    async function initialize() {
        await loadStyle("/sidebar.css");
        await loadSidebar();
        loadScript("/minisearch-6.1.0.min.js", setupSearch);
    }

    initialize();
};
