package org.dochub.idea.arch.tools;
// JBCefBrowser (https://intellij-support.jetbrains.com/hc/en-us/community/posts/4403677440146-how-to-add-a-JBCefBrowser-in-toolWindow-)

import com.fasterxml.jackson.databind.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.*;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.ui.jcef.*;
import com.intellij.util.messages.*;
import net.sourceforge.plantuml.code.ArobaseStringCompressor;
import net.sourceforge.plantuml.code.StringCompressor;
import org.cef.handler.CefLoadHandler;
import org.dochub.idea.arch.indexing.CacheBuilder;
import org.dochub.idea.arch.jsonschema.EntityManager;
import org.dochub.idea.arch.manifests.PlantUMLDriver;
import org.dochub.idea.arch.settings.SettingsState;
import org.dochub.idea.arch.wizard.RootManifest;
import org.apache.commons.io.*;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class DocHubToolWindow extends JBCefBrowser {
  private final JBCefJSQuery sourceQuery;
  private final Project project;
  private final Navigation navigation;
  private final JSGateway jsGateway;
  private static final StringCompressor stringCompressor = new ArobaseStringCompressor();
  private String html = null;
  public void reloadHtml(Boolean root) {
    SettingsState settingsState = SettingsState.getInstance();
    String currentURL = root ? ""
            : String.join("#",
                Arrays.copyOfRange(getCefBrowser().getURL().split("#")
                        , 0, 2));
    // Если используем корпоративный портал или портал в режиме DEV mode
    if (settingsState.isEnterprise()) {
      String url = currentURL.length() > 0 ? currentURL : settingsState.enterprisePortal;
      try {
        URIBuilder urlBuilder = new URIBuilder(url);
        urlBuilder.addParameter("$dochub-api-interface-func", sourceQuery.getFuncName());
        url = urlBuilder.toString() + "#/root/";
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
      loadURL(url);
    } else {
      // Если НЕ используем, то грузим из собственных ресурсов
      if (html == null) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("html/plugin.html")) {
          assert input != null;
          html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
          html = e.toString();
        }
        html = html.replaceAll("\\%\\$dochub-api-interface-func\\%", sourceQuery.getFuncName());
      }

      if (currentURL.length() > 0) {
        loadHTML(html, currentURL);
      } else {
        loadHTML(html, "file:///url=main");
      }
    }
  }

  private String getDirectFilePathOfURL(String url) {
    String basePath = project.getBasePath() + "/";
    String parentPath = (new File(CacheBuilder.getRootManifestName(project))).getParent();
    return basePath
            + (parentPath != null ? parentPath + "/" : "")
            + url.substring(20).split("\\?")[0];
  }

  private JBCefJSQuery.Response requestProcessing(String json) {
    // openDevtools();
    StringBuilder result = new StringBuilder();
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonObj = mapper.readTree(json);
      JsonNode jsonURL = jsonObj.get("url");
      if (jsonURL != null) {
        String url = jsonURL.asText();
        if (url.equals(Consts.ROOT_SOURCE_URI)) {
          Map<String, Object> response = new HashMap<>();
          String rootName = project.getBasePath() + "/" + CacheBuilder.getRootManifestName(project);
          File rootFile = new File(rootName);
          if (!rootFile.exists()) {
            return new JBCefJSQuery.Response("", 404, "No found: " + url);
          }
          response.put("contentType", rootName.substring(rootName.length() - 4).toLowerCase(Locale.ROOT));
          response.put("data", Files.readString(Path.of(rootName)));
          result.append(mapper.writeValueAsString(response));
        } else if (url.equals(Consts.PLANTUML_RENDER_SVG_URI)) {
          JsonNode jsonSource = jsonObj.get("source");
          String source = jsonSource != null ? jsonSource.asText() : "@startuml\n@enduml";
          source = stringCompressor.decompress(source);
          Map<String, Object> response = new HashMap<>();
          response.put("data", PlantUMLDriver.makeSVG(source));
          result.append(mapper.writeValueAsString(response));
        } else if (url.equals(Consts.NAVI_GOTO_SOURCE_URI)) {
          navigation.go(jsonObj);
        } else if (url.equals(Consts.WIZARD_INIT_URI)) {
          JsonNode jsonMode = jsonObj.get("mode");
          String mode = jsonMode != null ? jsonMode.asText() : "production";
          if (mode.equals("example")) {
            (new RootManifest()).createExampleManifest(project);
          } else {
            (new RootManifest()).createRootManifest(project);
          }
          reloadHtml(false);
        } else if ((url.length() > 23) && url.startsWith(Consts.METAMODEL_PATH)) {
          String sourcePath =  url.substring(23).split("\\?")[0];

          String path = Paths.get("metamodel", sourcePath).toString();

          if (SystemInfoRt.isWindows) path = path.replace('\\', '/');

          try(InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assert input != null;
            Map<String, Object> response = new HashMap<>();
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            response.put("data", content);
            String contentType = FilenameUtils.getExtension(path).toLowerCase(Locale.ROOT);
            response.put("contentType", contentType);
            result.append(mapper.writeValueAsString(response));
          } catch (IOException e) {
            return new JBCefJSQuery.Response("", 500, "Could not open source " + path + "\nError: " + e.toString());
          }
        } else if ((url.length() > 20) && url.startsWith(Consts.ROOT_SOURCE_PATH)) {
          String sourcePath = getDirectFilePathOfURL(url);
          File file = new File(sourcePath);
          if (!file.exists() || file.isDirectory()) {
            return new JBCefJSQuery.Response("", 404, "No found: " + url);
          }
          Map<String, Object> response = new HashMap<>();
          String contentType = FilenameUtils.getExtension(sourcePath).toLowerCase(Locale.ROOT);
          response.put("contentType", contentType);
          if (contentType.equals("jpg") || contentType.equals("jpeg") || contentType.equals("svg")
                  || contentType.equals("png")) {
            response.put("data", Files.readAllBytes(Path.of(sourcePath)));
          } else
            response.put("data", Files.readString(Path.of(sourcePath)));
          result.append(mapper.writeValueAsString(response));
        } else if (url.equals(Consts.ACTION_PULL_URI)) {
          Map<String, Object> response = new HashMap<>();
          response.put("contentType", "json");
          response.put("data", jsGateway.pullJSONMessage());
          result.append(mapper.writeValueAsString(response));
        } else if (url.equals(Consts.ACTION_DOWNLOAD_URI)) { // Сохранение файлов из WEB морды

          JsonNode jsonContent = jsonObj.get("content");
          JsonNode jsonTitle = jsonObj.get("title");
          JsonNode jsonDescription = jsonObj.get("description");
          JsonNode jsonExtension = jsonObj.get("extension");

          if (jsonContent != null) {
            Download.download(
                    jsonContent.asText(),
                    jsonTitle != null ? jsonTitle.asText() : "",
                    jsonDescription != null ? jsonDescription.asText() : "",
                    jsonExtension != null ? jsonExtension.asText(): ""
            );
          }
        } else if (url.equals(Consts.ACTION_PUSH_FILE)) { // Сохранение файлов из WEB морды
          JsonNode jsonContent = jsonObj.get("content");
          JsonNode jsonSource = jsonObj.get("source");
          if (jsonContent != null && jsonSource != null) {
            String sourcePath = getDirectFilePathOfURL(jsonSource.asText());
            Code.pushFile(project, sourcePath, jsonContent.asText());
          }
        } else if (url.equals(Consts.DEVTOOL_SHOW_URI)){
          openDevtools();
          getCefBrowser().executeJavaScript("console.info('GO!!');", "events.js", 0);
        } else if (url.equals(Consts.HTML_RELOAD_URI)){
          reloadHtml(false);
        } else if (url.equals(Consts.ENTITIES_APPLY_SCHEMA)) {

          JsonNode schema = jsonObj.get("schema");
          EntityManager.applySchema(project, schema.asText());

        } else if (url.equals(Consts.CLIPBOARD_COPY)) {
          Clipboard.copy(jsonObj.get("data").asText());
        } else if (url.equals(Consts.GET_SETTINGS)) {
          SettingsState settingsState = SettingsState.getInstance();
          Map<String, Object> response = new HashMap<>();
          response.put("isEnterprise", settingsState.isEnterprise());
          // todo Нужно перенести в какой-то конфиг
          response.put("api", "1.0.0");

          Map<String, Object> render = new HashMap<>();
          render.put("external", settingsState.renderIsExternal);
          render.put("server", settingsState.serverRendering);
          render.put("request_type", settingsState.renderServerRequestType);
          response.put("render", render);

          // Получаем переменные среды
          Map<String, Object> env = new HashMap<>();
          for (String envName : System.getenv().keySet()) {
            if(envName.startsWith("DOCHUB_IDE_")) {
              env.put(envName, System.getenv(envName));
            }
            // System.out.format("%s=%s%n", envName, env.get(envName));
          }

          // Если указаны параметры интеграции, передаем их
          if(settingsState.isEnterprise() && settingsState.gitServerMode.equals("Gitlab")) {
            env.put("DOCHUB_IDE_GITLAB_URL", settingsState.gitServer);
          } else if(settingsState.isEnterprise() && settingsState.gitServerMode.equals("Bitbucket")) {
            env.put("DOCHUB_IDE_BITBUCKET_URL", settingsState.gitServer);
          }

          if(!settingsState.gitPersonalToken.isEmpty()) {
            env.put("DOCHUB_IDE_PERSONAL_TOKEN", settingsState.gitPersonalToken);
          }

          response.put("env", env);
          result.append(mapper.writeValueAsString(response));
        } else {
          return new JBCefJSQuery.Response("", 404, "No found: " + url);
        }
      }
    } catch (IOException e1) {
      return new JBCefJSQuery.Response("", 500, e1.toString());
    }
    return new JBCefJSQuery.Response(result.toString());
  }
  public DocHubToolWindow(Project project) {
    super("/");

    PlantUMLDriver.init();

    ApplicationManager.getApplication().getMessageBus().connect().subscribe(SettingsState.ON_SETTING_CHANGED, new SettingsState.DocHubSettingsMessage() {
      @Override
      public void on() {
        reloadHtml(true);
      }
    });

    this.project = project;

    MessageBusConnection eventBus = project.getMessageBus().connect();
    navigation = new Navigation(project);
    jsGateway = new JSGateway(project);
    eventBus.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        String rootManifest = CacheBuilder.getRootManifestName(project);
        events.forEach(event -> {
          if (event.getFile() != null &&
                  (
                          event instanceof VFileContentChangeEvent
                          ||  event instanceof VFileDeleteEvent
                  )
          ) {
            String path = event.getFile().getPath();
            String basePath = project.getBasePath();
            if ((basePath == null) || (path == null) || !path.startsWith(basePath)) return;
            String source = path.substring(project.getBasePath().length() + 1);
            if (source.startsWith(".")) return;
            if (source.equals(rootManifest)) source = Consts.ROOT_SOURCE;
            jsGateway.appendMessage(Consts.ACTION_SOURCE_CHANGED, Consts.ROOT_SOURCE_PATH + source, null);
          }
        });
      }
    });
    sourceQuery = JBCefJSQuery.create((JBCefBrowserBase)this);
    sourceQuery.addHandler(this::requestProcessing);

    setErrorPage(new JBCefBrowserBase.ErrorPage() {
      @Override
      public @Nullable String create(CefLoadHandler.@NotNull ErrorCode errorCode,
                                     @NotNull String errorText,
                                     @NotNull String failedUrl) {
        return (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) ?
                null : JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl);
        }
    });

    reloadHtml(true);
  }
}
