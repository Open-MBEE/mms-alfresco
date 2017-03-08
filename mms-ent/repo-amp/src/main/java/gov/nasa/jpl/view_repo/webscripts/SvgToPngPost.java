package gov.nasa.jpl.view_repo.webscripts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.SvgToPngActionExecuter;
//import gov.nasa.jpl.view_repo.actions.HtmlToPdfActionExecuter;
//import gov.nasa.jpl.view_repo.actions.SvgToPngActionExecuter;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

public class SvgToPngPost extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(SvgToPngPost.class);

	public SvgToPngPost() {
		super();
	}

	public SvgToPngPost(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		SvgToPngPost instance = new SvgToPngPost(repository, services);
		return instance.executeImplImpl(req, status, cache);
	}

	@Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req,
			Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

		SvgToPngPost instance = new SvgToPngPost(repository, services);
		JSONObject result = instance.saveAndStartAction(req, status);
		appendResponseStatusInfo(instance);

		status.setCode(responseStatus.getCode());
		if (result == null) {
			model.put("res", createResponseJson());
		} else {
			try {
				if (!Utils.isNullOrEmpty(response.toString()))
					result.put("message", response.toString());
				model.put("res", NodeUtil.jsonToString(result, 2));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		printFooter(user, logger, timer);
		return model;
	}

	private JSONObject saveAndStartAction(WebScriptRequest req, Status status) {
		JSONObject postJson = new JSONObject();
		String user = AuthenticationUtil.getRunAsUser();
		EmsScriptNode context = NodeUtil.getUserHomeFolder(user, true);
		EmsScriptNode jobNode = null;
		String name = "EMS_SVG_TO_PNG";
		if (ActionUtil.jobExists(context, name))
			return postJson;

		jobNode = ActionUtil.getOrCreateJob(context, name, "cm:content",
				status, response, true);

		if (jobNode != null) {
			startAction(jobNode, postJson);
			return postJson;
		} else {
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
					"Couldn't create SVG to PNG job: %s", name);
			return null;
		}
	}

	public void startAction(EmsScriptNode jobNode, JSONObject postJson){
		ActionService actionService = services.getActionService();
		Action svgToPngAction = actionService.createAction(SvgToPngActionExecuter.NAME);
		services.getActionService().executeAction(svgToPngAction, jobNode.getNodeRef(), true, true);
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		return false;
	}

	public void process(){
		log("Converting SVGs to PNGs...");
		String origUser = null;
		try {
			// to run as admin
			origUser = AuthenticationUtil.getRunAsUser();
			log("Impersonating Admin user...");
            AuthenticationUtil.setRunAsUser("admin");

			List<EmsScriptNode> svgList = getSvgs();
			if (svgList != null) {
				log(String.format("%s SVGs found!", svgList.size()));
				List<EmsScriptNode> pngList = getPngs();
				if (pngList != null && pngList.size() > 0) {
					log(String.format("%s PNGs found!", pngList.size()));
					PngBst bst = buildPngBst(pngList);
					log("Iterating through SVG list...\n");
					for (EmsScriptNode ems : svgList) {
						if (!bst.find(new PngBstNode(ems))) {
							String name = ems.getName();
							log(String.format("No corresponding PNG found for %s", name));
							try {
								addPng(ems);
								log("\tConversion succeeded.");
							} catch (Throwable e) {
								log(String.format("\tFailed to add PNG file for %s! %s. %s\n", ems.getName(), e.getMessage(), e.getStackTrace().toString()));
							}
						}
					}
				}
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		finally{
			log("\nEnding impersonation...");
			AuthenticationUtil.setRunAsUser(origUser);
		}
	}

	protected List<EmsScriptNode> findEmsScriptNodeByType(String type,
			String pattern) throws Throwable {
		if (Utils.isNullOrEmpty(type))
			throw new NullArgumentException("Search Type");
		if (Utils.isNullOrEmpty(pattern))
			throw new NullArgumentException("Search Pattern");
		ArrayList<NodeRef> results = NodeUtil.findNodeRefsByType(pattern, type,
				services);
		List<EmsScriptNode> list = EmsScriptNode.toEmsScriptNodeList(results,
				services, null, null);
		return list;
	}

	protected List<EmsScriptNode> getSvgs() throws Throwable {
		log("Getting all SVGs...");
		//searching by name for now while searching by mimetype is not working
		//since our SVGs are named *.svg this should work
		return findEmsScriptNodeByType("@cm\\:name:\"",
				"*.svg");
		//return findEmsScriptNodeByType("@cm\\:content.mimetype:\"",
		//		"image/svg+xml");
	}

	protected List<EmsScriptNode> getPngs() throws Throwable {
		log("Getting all PNGs...");
		return findEmsScriptNodeByType("@cm\\:content.mimetype:\"", "image/png");
	}

	protected PngBst buildPngBst(List<EmsScriptNode> pngList) throws Throwable {
		log("Building PNGs binary search tree...");
		if (pngList == null)
			throw new NullArgumentException("PNG List");
		PngBst bst = new PngBst();
		for (EmsScriptNode ems : pngList) {
			bst.insert(ems);
		}
		//bst.display(bst.root);
		return bst;
	}

	protected void addPng(EmsScriptNode node) throws Throwable {
		if (node == null) throw new NullArgumentException("EmsScriptNode");
		log(String.format("\tAdding PNG for %s...", node.getName()));
		Path svgPath = saveSvgToFileSystem(node);
		Path pngPath = svgToPng(svgPath);
		String siteName = node.getSiteName(null, null);
		EmsScriptNode pngArtifact = NodeUtil.updateOrCreateArtifactPng(node, pngPath, siteName, null, null, null, response, null, false);
		if(pngArtifact == null){
			log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Failed to convert SVG to PNG!\n");
		}
		else{
			pngArtifact.getOrSetCachedVersion();
		}
		Files.deleteIfExists(svgPath);
		Files.deleteIfExists(pngPath);
	}

	protected Path saveSvgToFileSystem(EmsScriptNode node) throws Throwable {
		if (node == null) throw new NullArgumentException("EmsScriptNode");
		log(String.format("\tSaving %s to filesystem.", node.getName()));
		try{
			NodeRef nodeRef = node.getNodeRef();
			ContentReader imgReader = this.services.getContentService().getReader(
					nodeRef, ContentModel.PROP_CONTENT);
			File tempDir = TempFileProvider.getTempDir();
			Path filePath = Paths.get(tempDir.getAbsolutePath(), node.getName());
			File imgFile = new File(filePath.toString());
			imgReader.getContent(imgFile);
			return filePath;
		}
		catch(Throwable ex){
			throw new Throwable(String.format("Failed to save SVG to filesystem! %s", ex.getMessage()));
		}
	}

	protected Path svgToPng(Path svgPath) throws Throwable{
		log("\tConverting SVG to PNG...");
		Path pngPath = Paths.get(svgPath.toString().replace(".svg", ".png"));
		try(OutputStream png_ostream = new FileOutputStream(pngPath.toString()); ){
			String svg_URI_input = svgPath.toUri().toURL().toString();
			TranscoderInput input_svg_image = new TranscoderInput(svg_URI_input);
	        TranscoderOutput output_png_image = new TranscoderOutput(png_ostream);
	        PNGTranscoder my_converter = new PNGTranscoder();
	        my_converter.transcode(input_svg_image, output_png_image);
		}
		catch(Throwable ex){
			throw new Throwable("Failed to convert SVG to PNG! " + ex.getMessage());
		}
		return pngPath;
	}
}
