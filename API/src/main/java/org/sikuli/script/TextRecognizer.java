/*
 * Copyright (c) 2010-2019, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.script;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.Finder.Finder2;
import org.sikuli.script.support.RunTime;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

public class TextRecognizer {

  private static int lvl = 3;

  public static String versionTess4J = "4.4.1";
  public static String versionTesseract = "4.1.0";

  public ITesseract getTesseractAPI() {
    try {
      ITesseract tesseract = new Tesseract1();
      tesseract.setOcrEngineMode(options.oem());
      tesseract.setPageSegMode(options.psm());
      tesseract.setLanguage(options.language());
      tesseract.setDatapath(options.dataPath());
      for (Map.Entry<String, String> entry : options.variables().entrySet()) {
        tesseract.setTessVariable(entry.getKey(), entry.getValue());
      }
      if (!options.configs().isEmpty()) {
        tesseract.setConfigs(new ArrayList<>(options.configs()));
      }
      return tesseract;
    } catch (UnsatisfiedLinkError e) {
      String helpURL;
      if (RunTime.get().runningWindows) {
        helpURL = "https://github.com/RaiMan/SikuliX1/wiki/Windows:-Problems-with-libraries-OpenCV-or-Tesseract";
      } else {
        helpURL = "https://github.com/RaiMan/SikuliX1/wiki/macOS-Linux:-Support-libraries-for-Tess4J-Tesseract-4-OCR";
      }
      Debug.error("see: " + helpURL);
      if (RunTime.isIDE()) {
        Debug.error("Save your work, correct the problem and restart the IDE!");
        try {
          Desktop.getDesktop().browse(new URI(helpURL));
        } catch (IOException ex) {
        } catch (URISyntaxException ex) {
        }
      }
      throw new SikuliXception(String.format("OCR: start: Tesseract library problems: %s", e.getMessage()));
    }
  }

  private OCR.Options options;

  public OCR.Options options() {
    return options;
  }

  //<editor-fold desc="00 start, stop, reset">
  protected TextRecognizer() {
    this(OCR.globalOptions());
  }

  protected TextRecognizer(OCR.Options options) {
    RunTime.loadLibrary(RunTime.libOpenCV);
    this.options = options;
  }

  public static TextRecognizer start() {
    return start(OCR.globalOptions());
  }

  public static TextRecognizer start(OCR.Options options) {
    Debug.log(lvl, "OCR: start: Tess4J %s using Tesseract %s", versionTess4J, versionTesseract);
    if (options == null) {
      options = OCR.globalOptions();
    }
    TextRecognizer tr = new TextRecognizer(options);
    if (OCR.Options.defaultDataPath == null) {
      // export SikuliX eng.traineddata, if libs are exported as well
      File fTessDataPath = new File(RunTime.get().fSikulixAppPath, "SikulixTesseract/tessdata");
      boolean shouldExport = RunTime.get().shouldExport();
      boolean fExists = fTessDataPath.exists();
      if (!fExists || shouldExport) {
        if (0 == RunTime.get().extractResourcesToFolder("/tessdataSX", fTessDataPath, null).size()) {
          throw new SikuliXception(String.format("OCR: start: export tessdata did not work: %s", fTessDataPath));
        }
      }
      // if set, try with provided tessdata parent folder
      String defaultDataPath;
      if (Settings.OcrDataPath != null) {
        defaultDataPath = new File(Settings.OcrDataPath, "tessdata").getAbsolutePath();
      } else {
        defaultDataPath = fTessDataPath.getAbsolutePath();
      }
      if (options.dataPath() == null) {
        options.dataPath(defaultDataPath);
      }
      OCR.Options.defaultDataPath = defaultDataPath;
    }
    return tr;
  }

  public static void reset() {
    OCR.globalOptions().reset();
  }

  public static void status() {
    Debug.logp("Global settings " + OCR.globalOptions().toString());
  }

  public String toString() {
    return options.toString();
  }
  //</editor-fold>

  //<editor-fold desc="02 set OEM, PSM">

  /**
   * Sets the OEM
   *
   * @param oem
   * @return
   * @deprecated Use options().oem()
   */
  @Deprecated
  public TextRecognizer setOEM(OCR.OcrEngineMode oem) {
    return setOEM(oem.ordinal());
  }

  /**
   * Sets the OEM.
   * <p>
   * OCR Engine modes:
   * 0    Original Tesseract only.
   * 1    Cube only.
   * 2    Tesseract + cube.
   * 3    Default, based on what is available.
   *
   * @param oem
   * @return
   * @deprecated Use options().oem()
   */
  @Deprecated
  public TextRecognizer setOEM(int oem) {
    options().oem(oem);
    return this;
  }


  /**
   * Sets the PSM.
   *
   * @param psm
   * @return
   * @deprecated Use options().psm()
   */
  @Deprecated
  public TextRecognizer setPSM(OCR.PageSegMode psm) {
    return setPSM(psm.ordinal());
  }

  /**
   * Sets the PSM.
   * <p>
   * Page segmentation modes:
   * 0    Orientation and script detection (OSD) only.
   * 1    Automatic page segmentation with OSD.
   * 2    Automatic page segmentation, but no OSD, or OCR.
   * 3    Fully automatic page segmentation, but no OSD. (Default)
   * 4    Assume a single column of text of variable sizes.
   * 5    Assume a single uniform block of vertically aligned text.
   * 6    Assume a single uniform block of text.
   * 7    Treat the image as a single text line.
   * 8    Treat the image as a single word.
   * 9    Treat the image as a single word in a circle.
   * 10    Treat the image as a single character.
   * 11    Sparse text. Find as much text as possible in no particular order.
   * 12    Sparse text with OSD.
   * 13    Raw line. Treat the image as a single text line, bypassing hacks that are Tesseract-specific.
   *
   * @param psm
   * @return the textRecognizer instance
   * @deprecated Use options().psm()
   */
  @Deprecated
  public TextRecognizer setPSM(int psm) {
    options().psm(psm);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="03 set datapath, language, variable, configs">

  /**
   * Sets the dataPath
   *
   * @param dataPath
   * @return
   * @deprecated Use options().datapath()
   */
  @Deprecated
  public TextRecognizer setDataPath(String dataPath) {
    options().dataPath(dataPath);
    return this;
  }

  /**
   * Sets the OCR language
   *
   * @param language
   * @return
   * @deprecated Use options().language()
   */
  @Deprecated
  public TextRecognizer setLanguage(String language) {
    options().language(language);
    return this;
  }

  /**
   * Sets a Tesseract variable.
   *
   * @param key
   * @param value
   * @return
   * @deprecated Use options().variable(String key, String value)
   */
  @Deprecated
  public TextRecognizer setVariable(String key, String value) {
    options().variable(key, value);
    return this;
  }

  /**
   * Sets Tesseract configs
   *
   * @param configs
   * @return
   * @deprecated Use OCR.globalOptions.configs(String... configs)
   */
  @Deprecated
  public TextRecognizer setConfigs(String... configs) {
    setConfigs(Arrays.asList(configs));
    return this;
  }

  /**
   * Sets Tesseract configs
   *
   * @param configs
   * @return
   * @deprecated Use options.configs(List<String> configs)
   */
  @Deprecated
  public TextRecognizer setConfigs(List<String> configs) {
    options().configs(configs);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="10 image optimization">

  /**
   * Hint for the OCR Engine about the expected font size in pt globally
   *
   * @param size expected font size in pt
   * @deprecated Use options().setFontSize(int size)
   */
  public TextRecognizer setFontSize(int size) {
    options().setFontSize(size);
    return this;
  }

  /**
   * Hint for the OCR Engine about the expected height of an uppercase X in px globally.
   *
   * @param height of an uppercase X in px
   * @deprecated USe options().setTextHeight(int height)
   */
  public TextRecognizer setTextHeight(int height) {
    options().textHeight(height);
    return this;
  }

  public BufferedImage optimize(BufferedImage bimg) {
    Mat mimg = Finder2.makeMat(bimg);

    Imgproc.cvtColor(mimg, mimg, Imgproc.COLOR_BGR2GRAY);

    // sharpen original image to primarily get rid of sub pixel rendering artifacts
    mimg = unsharpMask(mimg, 3);

    float rFactor = options.factor();

    if (rFactor > 0 && rFactor != 1) {
      Image.resize(mimg, rFactor, options.resizeInterpolation());
    }

    // sharpen the enlarged image again
    mimg = unsharpMask(mimg, 5);

    // invert in case of mainly dark background
    if (Core.mean(mimg).val[0] < 127) {
      Core.bitwise_not(mimg, mimg);
    }

    return Finder2.getBufferedImage(mimg);
  }

  /*
   * sharpens the image using an unsharp mask
   */
  private Mat unsharpMask(Mat img, double sigma) {
    Mat blurred = new Mat();
    Imgproc.GaussianBlur(img, blurred, new Size(), sigma, sigma);
    Core.addWeighted(img, 1.5, blurred, -0.5, 0, img);
    return img;
  }
  //</editor-fold>

  //<editor-fold desc="20 OCR from Regions or images">
  public static final int PAGE_ITERATOR_LEVEL_WORD = 3;
  public static final int PAGE_ITERATOR_LEVEL_LINE = 2;

  public static <SFIRBS> String readText(SFIRBS from) {
    String text = doRead(from, OCR.globalOptions());
    return text;
  }

  public static <SFIRBS> String readText(SFIRBS from, OCR.Options options) {
    String text = doRead(from, options);
    return text;
  }

  public static <SFIRBS> List<Match> readLines(SFIRBS from) {
    return readLines(from, OCR.globalOptions());
  }

  public static <SFIRBS> List<Match> readLines(SFIRBS from, OCR.Options options) {
    BufferedImage bimg = getBufferedImage(from);
    return readTextItems(bimg, PAGE_ITERATOR_LEVEL_LINE, options);
  }

  public static <SFIRBS> List<Match> readWords(SFIRBS from) {
    return readWords(from, OCR.globalOptions());
  }

  public static <SFIRBS> List<Match> readWords(SFIRBS from, OCR.Options options) {
    BufferedImage bimg = getBufferedImage(from);
    return readTextItems(bimg, PAGE_ITERATOR_LEVEL_WORD, options);
  }
  //</editor-fold>

  //<editor-fold desc="30 helper">
  private static String doRead(Object from, OCR.Options options) {
    TextRecognizer tr = TextRecognizer.start(options);
    String text = "";
    BufferedImage bimg = getBufferedImage(from);
    try {
      text = tr.getTesseractAPI().doOCR(tr.optimize(bimg)).trim().replace("\n\n", "\n");
    } catch (TesseractException e) {
      Debug.error("OCR: read: Tess4J: doOCR: %s", e.getMessage());
      return "";
    }
    return text;
  }

  private static List<Match> readTextItems(BufferedImage bimg, int level, OCR.Options options) {
    List<Match> lines = new ArrayList<>();
    TextRecognizer tr = start(options);
    BufferedImage bimgResized = tr.optimize(bimg);
    List<Word> textItems = tr.getTesseractAPI().getWords(bimgResized, level);
    double wFactor = (double) bimg.getWidth() / bimgResized.getWidth();
    double hFactor = (double) bimg.getHeight() / bimgResized.getHeight();
    for (Word textItem : textItems) {
      Rectangle boundingBox = textItem.getBoundingBox();
      Rectangle realBox = new Rectangle(
          (int) (boundingBox.x * wFactor) - 1,
          (int) (boundingBox.y * hFactor) - 1,
          1 + (int) (boundingBox.width * wFactor) + 2,
          1 + (int) (boundingBox.height * hFactor) + 2);
      lines.add(new Match(realBox, textItem.getConfidence(), textItem.getText().trim()));
    }
    return lines;
  }

  private static BufferedImage getBufferedImage(Object whatEver) {
    if (whatEver instanceof String) {
      return Image.create((String) whatEver).get();
    } else if (whatEver instanceof File) {
      return Image.create((File) whatEver).get();
    } else if (whatEver instanceof Region) {
      Region reg = (Region) whatEver;
      return reg.getScreen().capture(reg).getImage();
    } else if (whatEver instanceof Image) {
      return ((Image) whatEver).get();
    } else if (whatEver instanceof ScreenImage) {
      return ((ScreenImage) whatEver).getImage();
    } else if (whatEver instanceof BufferedImage) {
      return (BufferedImage) whatEver;
    }
    throw new IllegalArgumentException(String.format("Illegal OCR source: %s", whatEver != null ? whatEver.getClass() : "null"));
  }

  /**
   * @return the current screen resolution in dots per inch
   * @deprecated Will be removed in future versions<br>
   * use Toolkit.getDefaultToolkit().getScreenResolution()
   */
  public int getActualDPI() {
    return Toolkit.getDefaultToolkit().getScreenResolution();
  }
  //</editor-fold>

  //<editor-fold desc="99 obsolete">

  /**
   * @param simg
   * @return the text read
   * @deprecated use readText() instead
   */
  @Deprecated
  public static String doOCR(ScreenImage simg) {
    return doOCR(simg.getImage());
  }

  /**
   * @param bimg
   * @return the text read
   * @deprecated use readText() instead
   */
  @Deprecated
  public static String doOCR(BufferedImage bimg) {
    String text = TextRecognizer.readText(bimg);
    return text;
  }

  /**
   * use start() instead
   *
   * @return
   */
  @Deprecated
  public static TextRecognizer getInstance() {
    TextRecognizer tr = TextRecognizer.start();
    return tr;
  }

  /**
   * deprecated use readText() instead
   *
   * @param simg
   * @return text
   */
  @Deprecated
  public String recognize(ScreenImage simg) {
    BufferedImage bimg = simg.getImage();
    return readText(bimg);
  }

  /**
   * deprecated use readText() instead
   *
   * @param bimg
   * @return text
   */
  @Deprecated
  public String recognize(BufferedImage bimg) {
    return readText(bimg);
  }
  //</editor-fold>

}
