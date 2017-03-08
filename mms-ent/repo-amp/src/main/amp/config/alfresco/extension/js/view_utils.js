function addElement(modelNode, seen, elements) {
    var info = {};
    info['mdid'] = modelNode.properties["view:mdid"];
    info['documentation'] = fixArtifactUrls(modelNode.properties["view:documentation"], false);
    var name = modelNode.properties["view:name"];
    if (name != null && name != undefined)
        info['name'] = name;
    var dvalue = modelNode.properties["view:defaultValue"];
    if (dvalue != null && dvalue != undefined)
        info['dvalue'] = dvalue;
    elements.push(info);
    seen.push(modelNode.properties['view:mdid']);
}


function handleView(view, seen, elements, views, view2view) {
    var sourcesJson = view.properties["view:sourcesJson"];
    var sources = JSON.parse(sourcesJson);
    for (var i in sources) {
        var sourceid = sources[i];
        var modelNode = getModelElement(modelFolder, sourceid); //modelFolder.childByNamePath(sourceid);
        if (modelNode == null)
            continue;
        if (seen.indexOf(sourceid) >= 0)
            continue;
        addElement(modelNode, seen, elements);
    }
    
    var viewinfo = {};
    viewinfo['mdid'] = view.properties['view:mdid'];
    viewinfo['noSection'] = view.properties['view:noSection'];
    viewinfo['contains'] = JSON.parse(fixArtifactUrls(view.properties['view:containsJson'],true));
    
    var viewcomments = [];
    var comments = view.assocs['view:comments'];
    for (var i in comments) {
        var comment = comments[i];
        if (comment.properties['view:deleted'])
            continue;
        var commentinfo = {};
        commentinfo['author'] = comment.properties['view:author'];
        commentinfo['modified'] = utils.toISO8601(comment.properties['view:lastModified']);
        commentinfo['id'] = comment.properties["view:mdid"];
        commentinfo['body'] = comment.properties['view:documentation'];
        viewcomments.push(commentinfo);
    }
    viewinfo['comments'] = viewcomments.sort(function(a,b) {
        if (a.modified < b.modified)
            return -1;
        if (a.modified == b.modified)
            return 0;
        return 1;
    });
    
    viewinfo['author'] = view.properties['cm:modifier'];
    viewinfo['modified'] = utils.toISO8601(view.properties['cm:modified']);
    views.push(viewinfo);
    
    if (!product) {
        view2view[view.properties['view:mdid']] = JSON.parse(view.properties['view:viewsJson']);
        var childViews = view.assocs["view:views"];
        for (var i in childViews) {
            handleView(childViews[i], seen, elements, views, view2view);
        }
    }
    if (view.hasPermission("Write"))
        viewinfo['editable'] = true;
    else
        viewinfo['editable'] = false;
    return viewinfo;
}

function getSnapshots(topview) {
    var snapshots = [];
    var ss = topview.assocs["view:snapshots"];
    for (var i in ss) {
        //utils.toISO8601(date);
        var snapshot = ss[i];
        /*var html = snapshot.assocs["view:html"];
        if (html.length > 0)
            html = html[0];
        else
            continue;*/
        snapshots.push({
            "id": snapshot.properties["cm:name"], 
            "created": utils.toISO8601(snapshot.properties["cm:created"]),
            "url": url.context + "/wcs/ui/views/" + topview.properties["view:mdid"] + "/snapshots/" + snapshot.properties["cm:name"],//html.url,
            "creator": snapshot.properties["cm:creator"]
        });
    }
    snapshots.sort(function(a,b) {
        if (a.created < b.created)
            return -1;
        if (a.created == b.created)
            return 0;
        return 1;
    })
    return snapshots;
}