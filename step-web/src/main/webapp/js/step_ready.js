(function () {

    function initDataSources() {
        //format the versions correctly
        step.keyedVersions = {};
        step.itemisedVersions = [];
        for (var ii = 0; ii < window.tempVersions.length; ii++) {
            var tempVersion = window.tempVersions[ii];
            var item = { item: tempVersion, itemType: 'version' };
            step.itemisedVersions.push(item);
            step.keyedVersions[tempVersion.initials] = tempVersion;
            step.keyedVersions[tempVersion.shortInitials] = tempVersion;
        }

        //now mark some versions as recommended
        markAsRecommended('OHB');
        markAsRecommended('WHNU');
        markAsRecommended('LXX');
        markAsRecommended('SBLG');

        //save 100k of space
        window.tempVersions = null;
    };

    function markAsRecommended(version) {
        var v = step.keyedVersions[version];
        if (v) {
            v.recommended = true;
        }
    }

    function initSettings() {
        var settings = new SettingsModelList;
        settings.fetch();
        if (settings.length == 0) {
            var setting = new SettingsModel;
            settings.add(setting);
            setting.save();
        }
        step.settings = settings.at(0);

        //override some particular settings to avoid UI shifting on load:
        //we never open up a related words section
        step.settings.save({ relatedWordsOpen: false });
    };

    function initSearchDropdown() {
        //setup search view
        var searchView = new MainSearchView();
        //we will sync the URL on load, if and only if there are arguments, i.e. not the home page...
        //to be firmed up
        searchView.syncWithUrl(step.util.activePassage());
    }

    function patchBackboneHistory() {
        //override history in backbone
        Backbone.history = _.extend(Backbone.history, {
            getFragment: function (fragment) {
                if (fragment == null) {
                    var path = window.location.pathname;
                    if (path && path[0] == '/') {
                        path = path.slice(1);
                    }
                    var query = window.location.search;
                    if (query) {
                        path += query;
                    }
                    return path;
                }
                return fragment.replace(/^[#\/]|\s+$/g, '');
            }});
    }

    function initCoreModelsAndRouter() {
        step.router = new StepRouter();
        step.passages = new PassageModelList();
        step.passages.fetch();
        step.bookmarks = new HistoryModelList();
        step.bookmarks.fetch();

        //need to clean up passages... Ideally, by changing the values of passageIds to be 1,2,3,4,...
        //we reserve 0 for the first column
        for (var ii = 0; ii < step.passages.length; ii++) {
            //start at 1, and go onwards from then
            var p = step.passages.at(ii);
            p.save({
                passageId: ii + 1,
                pageNumber: 1,
                results: p.get("firstPageResults"),
                linked: null
            }, {
                silent: true 
            });
        }

        //now passage 0 is the one from the URL
        if (window.tempModel) {
            //because of page size, the 'value' is empty, so we'll need to put this back into the model after everything is over
            var pageValue = $(".passageContainer").find(".passageContent").html().trim();

            //now we can create the correct views
            var modelZero = new PassageModel({ passageId: 0 });
            step.passages.add(modelZero);

            //reset some attributes that weren't on the model to start with (because of space reasons)
            modelZero.save(window.tempModel);
            modelZero.save({ results: null, linked: null, value: pageValue}, {silent: true});
            new PassageMenuView({
                model: modelZero
            });

            step.router.handleRenderModel(modelZero, true);

            $(".helpMenuTrigger").one('click', function () {
                require(["view_help_menu"], function () {
                    new ViewHelpMenuOptions({});
                });
            });
        }
        if (step.passages.length == 0) {
            step.passages.add(new PassageModel({ passageId: 0 }));
        }
    }

    //can this be done before load? self executing function
    $(window).on("load", function () {
        //disable amd
        define.amd = null;

        window.step = window.step || {};
        initSettings();
        initDataSources();
        patchBackboneHistory();
        initCoreModelsAndRouter();
        initSearchDropdown();
        Backbone.history.start({pushState: true, silent: true });

        new FeedbackView();
        if (step.passages.length > 1) {
            //we restore previous passages
            new RestorePassageView();
        }
    });
})();
