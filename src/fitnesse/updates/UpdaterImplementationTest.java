package fitnesse.updates;

import fitnesse.FitNesseContext;
import fitnesse.wiki.FileSystemPage;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.WikiPage;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;
import static util.RegexTestCase.assertSubString;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class UpdaterImplementationTest {
  private File updateList;
  private File updateDoNotCopyOver;
  public static final String testDir = "testDir";
  public static final String rootName = "RooT";

  protected WikiPage root;
  protected Update update;
  protected UpdaterImplementation updater;
  protected WikiPage pageOne;
  protected WikiPage pageTwo;
  protected FitNesseContext context;
  protected PageCrawler crawler;
  private boolean updateDone = false;

  @Before
  public void setUp() throws Exception {
    context = new FitNesseContext();
    context.rootPath = testDir;
    context.rootDirectoryName = rootName;
    context.rootPagePath = testDir + "/" + rootName;

    FileUtil.makeDir(testDir);
    root = new FileSystemPage(context.rootPath, context.rootDirectoryName);
    crawler = root.getPageCrawler();
    context.root = root;


    File classes = new File("classes");
    classes.mkdir();
    File resources = new File(classes, "Resources");
    resources.mkdir();
    File root = new File(resources, "FitNesseRoot");
    root.mkdir();
    File files = new File(resources, "files");
    files.mkdir();
    File testFile = new File(files, "TestFile");
    File bestFile = new File(files, "BestFile");
    File testRootFile = new File(root, "TestrootFile");
    File specialFile = new File(resources, "SpecialFile");
    testFile.createNewFile();
    bestFile.createNewFile();
    testRootFile.createNewFile();
    specialFile.createNewFile();
    updateList = new File("classes/Resources/updateList");
    updateDoNotCopyOver = new File("classes/Resources/updateDoNotCopyOverList");
    FileUtil.createFile(updateList, "files/TestFile\nfiles/BestFile\n");
    FileUtil.createFile(updateDoNotCopyOver, "SpecialFile");
    updater = new UpdaterImplementation(context);
  }

  @Test
  public void shouldBeAbleToGetUpdateFilesAndMakeAlistFromThem() throws Exception {
    ArrayList<String> updateArrayList = new ArrayList<String>();
    updater.tryToParseTheFileIntoTheList(updateList, updateArrayList);
    assertEquals("files/TestFile", updateArrayList.get(0));
    assertEquals("files/BestFile", updateArrayList.get(1));
    updateArrayList = new ArrayList<String>();
    updater.tryToParseTheFileIntoTheList(updateDoNotCopyOver, updateArrayList);
    assertEquals("SpecialFile", updateArrayList.get(0));
  }

  @Test
  public void shouldBeAbleToGetThePathOfJustTheParent() throws Exception {
    String filePath = updater.getCorrectPathForTheDestination("classes/files/moreFiles/TestFile");
    assertSubString("classes/files/moreFiles", filePath);
  }

  @Test
  public void shouldCreateTheCorrectPathForGivenPath() throws Exception {
    String filePath = updater.getCorrectPathFromJar("FitNesseRoot/files/moreFiles/TestFile");
    assertEquals("Resources/FitNesseRoot/files/moreFiles/TestFile", filePath);
  }

  @Test
  public void shouldCreateSomeFilesInTheRooTDirectory() throws Exception {
    for (Update update : updater.updates) {
      if (update.getClass() == ReplacingFileUpdate.class || update.getClass() == FileUpdate.class)
        update.doUpdate();
    }
    File testFile = new File(context.rootPath, "files/TestFile");
    File bestFile = new File(context.rootPath, "files/BestFile");
    File specialFile = new File(context.rootPath, "specialFile");
    assertTrue(testFile.exists());
    assertTrue(bestFile.exists());
    assertTrue(specialFile.exists());
  }

  @Test
  public void shouldReplaceFitNesseRootWithDirectoryRoot() throws Exception {
    String filePath = "FitNesseRoot/someFolder/someFile";
    context.rootDirectoryName = "MyNewRoot";
    String updatedPath = updater.getCorrectPathForTheDestination(filePath);
    assertEquals("MyNewRoot/someFolder", updatedPath);

  }

  @Test
  public void updatesShouldBeRunIfCurrentVersionNotAlreadyUpdated() throws Exception {
    String version = "TestVersion";
    updater.setFitNesseVersion(version);
    updater.testing = true;

    File propertiesFile = new File("testDir/RooT/properties");
    FileUtil.deleteFile(propertiesFile);
    assertFalse(propertiesFile.exists());

    updater.updates = new Update[]{
      new UpdateSpy()
    };
    updater.update();
    assertTrue(updateDone);
    assertTrue(propertiesFile.exists());

    Properties properties = updater.loadProperties();
    assertTrue(properties.containsKey("Version"));
    assertEquals(version, properties.get("Version"));
    FileUtil.deleteFile(propertiesFile);    
  }

  @Test
  public void updatesShouldNotBeRunIfCurrentVersionAlreadyUpdated() throws Exception {
    String version = "TestVersion";
    updater.setFitNesseVersion(version);
    Properties properties = updater.getProperties();
    properties.put("Version", version);
    updater.updates = new Update[]{
      new UpdateSpy()
    };
    updater.update();
    assertFalse(updateDone);
  }

  @After
  public void tearDown() {
    FileUtil.deleteFileSystemDirectory("classes/Resources");
    FileUtil.deleteFileSystemDirectory("testDir");
  }

  private class UpdateSpy implements Update {
    public String getName() {
      return "test";
    }

    public String getMessage() {
      return "test";
    }

    public boolean shouldBeApplied() throws Exception {
      return true;
    }

    public void doUpdate() throws Exception {
      updateDone = true;
    }
  }
}
