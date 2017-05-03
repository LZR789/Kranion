/* 
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.kranion.view;

import org.fusfoundation.kranion.model.image.io.Loader;
import org.fusfoundation.kranion.Landmark;
import org.fusfoundation.kranion.FloatParameter;
import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.Slider;
import org.fusfoundation.kranion.Trackball;
import org.fusfoundation.kranion.InsightecTxdrGeomReader;
import org.fusfoundation.kranion.HistogramChartControl;
import org.fusfoundation.kranion.Ring;
import org.fusfoundation.kranion.Canvas2DLayoutManager;
import org.fusfoundation.kranion.Scene;
import org.fusfoundation.kranion.ImageCanvas2D;
import org.fusfoundation.kranion.DirtyFollower;
import org.fusfoundation.kranion.TextBox;
import org.fusfoundation.kranion.RenderLayer;
import org.fusfoundation.kranion.ProgressBar;
import org.fusfoundation.kranion.XYChartControl;
import org.fusfoundation.kranion.Transducer;
import org.fusfoundation.kranion.TransformationAdapter;
import org.fusfoundation.kranion.OrientationAnimator;
import org.fusfoundation.kranion.FloatAnimator;
import org.fusfoundation.kranion.ShaderProgram;
import org.fusfoundation.kranion.ScreenTransition;
import org.fusfoundation.kranion.ImageHistogram;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.Cylinder;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.CoordinateWidget;
import org.fusfoundation.kranion.ImageCanvas3D;
import org.fusfoundation.kranion.TransferFunctionDisplay;
import org.fusfoundation.kranion.CrossHair;
import org.fusfoundation.kranion.RenderableAdapter;
import org.fusfoundation.kranion.RenderList;
import org.fusfoundation.kranion.PullDownSelection;
import org.fusfoundation.kranion.PlyFileReader;
import org.fusfoundation.kranion.TransducerRayTracer;
import org.fusfoundation.kranion.ImageLabel;
import org.fusfoundation.kranion.RadioButtonGroup;
import java.awt.Desktop;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import org.lwjgl.BufferUtils;
import java.nio.*;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.util.regex.Pattern;

import org.lwjgl.util.vector.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import java.lang.reflect.Constructor;



import java.io.File;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.Observable;
import javax.swing.*;

import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.model.image.*;
import org.fusfoundation.kranion.controller.Controller;

import org.knowm.xchart.Histogram;

import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;



public class DefaultView extends View {

    @Override
    public void percentDone(String msg, int percent) {
        System.out.println(msg + " - " + percent + "%");
        statusBar.setValue(percent);
        
        if (Thread.currentThread() == this.myThread) {
                    Main.update();
        }
    }

    public static final int DISPLAY_HEIGHT = 1024;
    public static final int DISPLAY_WIDTH = 1680;
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    private final org.fusfoundation.kranion.UpdateEventQueue updateEventQueue = new org.fusfoundation.kranion.UpdateEventQueue();

    private float cameraZ = -600f;
    private Trackball trackball = new Trackball(DISPLAY_HEIGHT / 2, DISPLAY_WIDTH / 2, DISPLAY_HEIGHT / 2f);
    private Trackball registerBallCT = new Trackball(DISPLAY_HEIGHT / 2, DISPLAY_WIDTH / 2, DISPLAY_HEIGHT / 2f);
    private Quaternion ctQstart, mrQstart;
    private Vector3f startCtImageTranslate = new Vector3f();
//    private Vector3f startMrImageTranslate = new Vector3f();
    private int mouseStartX, mouseStartY;

    private int selTrans, drawingStyle;
    private boolean doClip = false;
    private boolean doFrame = false;
    private boolean doMRI = false;
    private boolean showScanner = false;
    private FloatParameter dolly = new FloatParameter(-100f);
    private float transducerTilt = 0f;
    private float viewportAspect = 1f;
    
    private boolean attenuation_term_on = false ;
    private boolean transmissionLoss_term_on = false ;

    private Loader ctloader, mrloader;
//    private ImageVolume4D ctImage, mrImage;
    
    private Transducer transducerModel;
    private PlyFileReader stereotacticFrame = new PlyFileReader("/org/fusfoundation/kranion/meshes/Frame1v3.ply");
    private PlyFileReader mrBore2 = new PlyFileReader("/org/fusfoundation/kranion/meshes/Bore.ply");
    private PlyFileReader mrHousing = new PlyFileReader("/org/fusfoundation/kranion/meshes/Housing.ply");
    private PlyFileReader mrPedestal = new PlyFileReader("/org/fusfoundation/kranion/meshes/Pedestal.ply");
    private Landmark currentTarget = new Landmark();
    private Landmark currentSteering = new Landmark();
    private Cylinder mrBore, mrBoreOuter;
    private Ring mrBoreFront, mrBoreBack;
    private TransformationAdapter mrBoreTransform, frameTransform, frameOffsetTransform, steeringTransform;
    
    private RenderList mrBoreGroup = new RenderList();
    
    private int center = 150, window = 160, ct_threshold = 900;
    private int mr_center = 150, mr_window = 160, mr_threshold = 125;
    private ImageCanvas3D canvas = new ImageCanvas3D();
    private ImageCanvas2D canvas1 = new ImageCanvas2D();
    private ImageCanvas2D canvas2 = new ImageCanvas2D();
    private ImageCanvas2D canvas3 = new ImageCanvas2D();
    
    private Canvas2DLayoutManager mprLayout = new Canvas2DLayoutManager(canvas1, canvas2, canvas3);
        
    private TransducerRayTracer transRayTracer = new TransducerRayTracer();
    private boolean showRayTracer = false;
    
    private ProgressBar statusBar = new ProgressBar();
    private ProgressBar activeElementsBar = new ProgressBar();
    private ProgressBar sdrBar = new ProgressBar();
    private TransferFunctionDisplay transFuncDisplay = new TransferFunctionDisplay();
    private DirtyFollower transFuncDisplayProxy = new DirtyFollower(transFuncDisplay);
    private ImageHistogram ctHistogram = new ImageHistogram();
    
    private XYChartControl thermometryChart;
    private HistogramChartControl incidentAngleChart, sdrChart;
    
//    private Framebuffer overlay = new Framebuffer();
//    private Framebuffer overlayMSAA = new Framebuffer();
//    private Framebuffer mainLayerMSAA = new Framebuffer();
//    private Framebuffer mainLayer = new Framebuffer();
    
    private RenderLayer background = new RenderLayer(1);
    private RenderLayer mainLayer = new RenderLayer(8);
    private RenderLayer overlay = new RenderLayer(8);
    
    private FlyoutPanel flyout1 = new FlyoutPanel();
    private FlyoutPanel flyout2 = new FlyoutPanel();
    private FlyoutPanel flyout3 = new FlyoutPanel();
    
    private PullDownSelection sonicationSelector;
    private PullDownSelection mrSeriesSelector;
    
    
    private Scene scene = new Scene();
    private ScreenTransition transition = new ScreenTransition();
    private boolean doTransition = false;
    private float transitionTime = 0.5f;
    
    private enum mouseMode {
        SCENE_ROTATE,
        HEAD_ROTATE,
        HEAD_TRANSLATE,
        SKULL_ROTATE,
        SKULL_TRANSLATE,
        FRAME_ROTATE,
        FRAME_TRANSLATE
    }
    
    private mouseMode currentMouseMode = mouseMode.SCENE_ROTATE;
    
    private OrientationAnimator orientAnimator = new OrientationAnimator();
    private FloatAnimator zoomAnimator = new FloatAnimator();
        
    // Game controller components
    private static net.java.games.input.ControllerEnvironment controllerEnvironement;
    private static net.java.games.input.Controller gameController=null;

    public DefaultView() {
        selTrans = 0;
        drawingStyle = 0;
    }

    @Override
    public void create() throws LWJGLException {
        
        initController();
        
        mprLayout.setTag("mprLayout");
        
        canvas1.setOrientation(0);
        canvas2.setOrientation(1);
        canvas3.setOrientation(2);
        
        FlyoutPanel.setGuiScale(Display.getWidth()/1980f);
        
        flyout1.setBounds(0, 350, 400, 600);
        flyout1.setFlyDirection(FlyoutPanel.direction.EAST);

        TextBox textbox = (TextBox)new TextBox(225, 400, 100, 25, "", controller).setTitle("Acoustic Power").setCommand("sonicationPower");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(225, 370, 100, 25, "", controller).setTitle("Duration").setCommand("sonicationDuration");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(225, 330, 100, 25, "", controller).setTitle("Frequency").setCommand("sonicationFrequency");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(100, 450, 60, 25, "", controller).setTitle("R").setCommand("sonicationRLoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(200, 450, 60, 25, "", controller).setTitle("A").setCommand("sonicationALoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(300, 450, 60, 25, "", controller).setTitle("S").setCommand("sonicationSLoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        thermometryChart = new XYChartControl(10, 100, 380, 200);
        thermometryChart.setCommand("currentOverlayFrame");
        thermometryChart.addActionListener(this);
        flyout1.addChild(thermometryChart);
        
        flyout1.addChild(new Button(Button.ButtonType.TOGGLE_BUTTON, 200, 50, 180, 25, controller).setTitle("Show Thermometry").setCommand("showThermometry"));
        
        flyout1.addChild(new Button(Button.ButtonType.BUTTON, 10, 10, 100, 25, controller).setTitle("Update").setCommand("OPEN"));
        flyout1.addChild(new Button(Button.ButtonType.BUTTON,10, 50, 100, 25, controller).setTitle("Add").setCommand("CLOSE"));
        
        flyout1.addChild(sonicationSelector = (PullDownSelection)new PullDownSelection(10, 550, 380, 25, controller).setTitle("Sonication").setCommand("currentSonication"));
        sonicationSelector.setPropertyPrefix("Model.Attribute");
        model.addObserver(sonicationSelector);
        
        flyout2.setBounds(0, Display.getHeight()-300, Display.getWidth(), 300);
        flyout2.setAutoExpand(true);
        flyout2.setFlyDirection(FlyoutPanel.direction.SOUTH);
        flyout2.setTag("MainFlyout");
                
        Button button = new Button(Button.ButtonType.TOGGLE_BUTTON, 10, 25, 120, 25, controller);
        button.setTitle("Raytracer");
        button.setCommand("showRayTracer");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild(button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 170, 25, 120, 25, controller);
        button.setTitle("Clip");
        button.setCommand("doClip");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild(button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 170, 60, 120, 25, controller);
        button.setTitle("Frame");
        button.setCommand("doFrame");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild(button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 10, 60, 120, 25, controller);
        button.setTitle("Imaging");
        button.setCommand("doMRI");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild(button);

//        // new added part
//         button = new Button(Button.ButtonType.TOGGLE_BUTTON, 400, 205, 240, 25, controller);
//        button.setTitle("Attenuation Term");
//        button.setCommand("AttenuationTerm");
//        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        model.addObserver(button);
//        flyout2.addChild(button);
//        
//        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 400, 170, 240, 25, controller);
//        button.setTitle("Transmission Loss Term");
//        button.setCommand("TransmissionLossTerm");
//        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        model.addObserver(button);
//        flyout2.addChild(button);
//            
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 400, 250, 240, 25, controller).setTitle("Pressure Calc").setCommand("PressureCompute"));
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 720, 160, 100, 25, controller).setTitle("Ellipse +").setCommand("EllipsePlus"));
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 830, 160, 100, 25, controller).setTitle("Ellipse -").setCommand("EllipseMinus"));
//             

      
        
        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 10, 250, 200, 25, controller).setTitle("Load CT...").setCommand("loadCT"));
        flyout2.addChild(new Button(Button.ButtonType.BUTTON,10, 215, 200, 25, controller).setTitle("Load MR...").setCommand("loadMR"));
                
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 350, 250, 220, 25, controller).setTitle("Find Fiducials").setCommand("findFiducials"));
        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 350, 215, 220, 25, this).setTitle("Save Skull Params").setCommand("saveSkullParams"));
        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 350, 180, 220, 25, this).setTitle("Save ACT file").setCommand("saveACTfile"));
        
        Slider slider1 = new Slider(800, 75, 410, 25, controller);
        slider1.setTitle("Bone speed");
        slider1.setCommand("boneSOS"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(1482, 3500);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f m/s");
        slider1.setCurrentValue(2652);
        flyout2.addChild(slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(800, 25, 410, 25, controller);
        slider1.setTitle("Bone refraction speed");
        slider1.setCommand("boneRefractionSOS"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(1482, 3500);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f m/s");
        slider1.setCurrentValue(2652);
        flyout2.addChild(slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(800, 200, 410, 25, controller);
        slider1.setTitle("Transducer tilt");
        slider1.setCommand("transducerXTilt"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(-45, 45);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f degrees");
        slider1.setCurrentValue(0);
        flyout2.addChild(slider1);
        model.addObserver(slider1);
        
               // for the slider of the ellipse function
//        slider1 = new Slider(720, 200, 450, 25, controller);
//        slider1.setTitle("Ellipse Ratio:");
//        slider1.setCommand("EllipseShapechange"); // controller will set command name as propery on model
//        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        slider1.setMinMax(75, 100);
//        slider1.setLabelWidth(120);
//        slider1.setFormatString("%3.0f ");
//        slider1.setCurrentValue(0);
//        flyout2.addChild(slider1);
//        model.addObserver(slider1);
        
        slider1 = new Slider(800, 130, 410, 25, controller);
        slider1.setTitle("VR Slicing");
        slider1.setCommand("foregroundVolumeSlices"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 600);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%3.0f");
        slider1.setCurrentValue(0);
        flyout2.addChild(slider1);
        model.addObserver(slider1);
        
        flyout3.setBounds(Display.getWidth() - 400, 200, 400, 375);
        flyout3.setFlyDirection(FlyoutPanel.direction.WEST);
        
        
        slider1 = new Slider(10, 150, 380, 25, controller);
        slider1.setTitle("MR Center");
        slider1.setCommand("MRcenter"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild(slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 90, 380, 25, controller);
        slider1.setTitle("MR Window");
        slider1.setCommand("MRwindow"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild(slider1);
        model.addObserver(slider1);

        slider1 = new Slider(10, 30, 380, 25, controller);
        slider1.setTitle("MR Thresh");
        slider1.setCommand("MRthresh"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 1024);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild(slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 270, 380, 25, controller);
        slider1.setTitle("CT Center");
        slider1.setCommand("CTcenter"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild(slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 210, 380, 25, controller);
        slider1.setTitle("CT Window");
        slider1.setCommand("CTwindow"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild(slider1);
        model.addObserver(slider1);
        
//                flyout2.addChild(new ImageLabel("images/3dmoveicon.png", 350, 50, 200, 200));
//                flyout2.addChild(new ImageLabel("images/3drotateicon.png", 425, 50, 200, 200));
                
                RadioButtonGroup buttonGrp1 = new RadioButtonGroup();
                
                Button radio1 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 25, 150, 25, this);
                radio1.setTitle("Rotate Skull");
                radio1.setCommand("rotateSkull");
                
//                Button radio2 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 60, 150, 25, this);
//                radio2.setTitle("Rotate Head");
//                radio2.setCommand("rotateHead");
                
//                Button radio3 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 95, 150, 25, this);
//                radio3.setTitle("Rotate Frame");
//                radio3.setCommand("rotateFrame");
                
                Button radio4 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 60/*130*/, 150, 25, this);
                radio4.setTitle("Rotate Scene");
                radio4.setCommand("rotateScene");
                radio4.setIndicator(true);
            
                
                Button radio5 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 25, 150, 25, this);
                radio5.setTitle("Translate Skull");
                radio5.setCommand("translateSkull");
                
                Button radio6 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 60, 150, 25, this);
                radio6.setTitle("Translate Head");
                radio6.setCommand("translateHead");
                
//                Button radio7 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 95, 150, 25, this);
//                radio7.setTitle("Translate Frame");
//                radio7.setCommand("translateFrame");
                


                radio1.setDrawBackground(false);
//                radio2.setDrawBackground(false);
//                radio3.setDrawBackground(false);
                radio4.setDrawBackground(false);
                radio5.setDrawBackground(false);
                radio6.setDrawBackground(false);
//                radio7.setDrawBackground(false);
                
                buttonGrp1.addChild(radio1);
//                buttonGrp1.addChild(radio2);
//                buttonGrp1.addChild(radio3);
                buttonGrp1.addChild(radio4);
                buttonGrp1.addChild(radio5);
                buttonGrp1.addChild(radio6);
//                buttonGrp1.addChild(radio7);
                
                flyout2.addChild(buttonGrp1);

        
        flyout3.addChild(mrSeriesSelector = (PullDownSelection)new PullDownSelection(10, 325, 380, 25, controller).setTitle("MR Series").setCommand("selectMRSeries"));
        
        mrBore = new Cylinder(300.0f, 50.0f, -25.0f, -1f);
        mrBoreOuter = new Cylinder(310.0f, 50.0f, -25.0f, 1f);
        mrBoreFront = new Ring(310.0f, 10.0f, -25.0f, -1f);
        mrBoreBack = new Ring(310.0f, 10.0f, 25.0f, 1f);

        mrBore.setColor(0.7f, 0.7f, 0.8f);
        mrBoreOuter.setColor(0.7f, 0.7f, 0.8f);
        mrBoreFront.setColor(0.7f, 0.7f, 0.8f);
        mrBoreBack.setColor(0.7f, 0.7f, 0.8f);
        
        mrBoreGroup.add(mrBore);
        mrBoreGroup.add(mrBoreOuter);
        mrBoreGroup.add(mrBoreFront);
        mrBoreGroup.add(mrBoreBack);
        mrBoreGroup.setClipColor(0.7f, 0.7f, 0.8f, 1f);

        
        try {
            System.out.println(Transducer.getTransducerDefCount() + " transdcuer definitions found.");
            transducerModel = new Transducer(0);

//            transducer220.setTrackball(trackball);

            stereotacticFrame.readObject();
            stereotacticFrame.setColor(0.65f, 0.65f, 0.65f, 1f);
            
            mrBore2.readObject();
            mrBore2.setColor(0.80f, 0.80f, 0.80f, 1f);
            mrHousing.readObject();
            mrHousing.setColor(0.55f, 0.55f, 0.55f, 1f);
            mrPedestal.readObject();
            mrPedestal.setColor(0.25f, 0.25f, 0.25f, 1f);
                               
            ShaderProgram shader = new ShaderProgram();
            shader.addShader(GL_VERTEX_SHADER, "/org/fusfoundation/kranion/shaders/Collision.vs.glsl");
            shader.addShader(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/Collision.fs.glsl");
            shader.compileShaderProgram();
            stereotacticFrame.setShader(shader);
        

        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        
        transRayTracer.init(transducerModel);
        transRayTracer.addObserver(this);
        
        statusBar.setMinMax(0, 100);
        statusBar.setBounds(550, 500, 600, 30);
        statusBar.setFormatString("Status: %3.0f");
        statusBar.setFontSize(18);
        statusBar.setGreenLevel(0.0f);
        statusBar.setYellowLevel(0.0f);
        statusBar.setTag("statusBar");
        
        activeElementsBar.setMinMax(0, 1024);
        activeElementsBar.setBounds(300, 201, 500, 20);
        activeElementsBar.setFormatString("Elements: %3.0f");
        activeElementsBar.setFontSize(14);
        activeElementsBar.setGreenLevel(0.7f);
        activeElementsBar.setYellowLevel(0.5f);
        
        sdrBar.setMinMax(0f, 1f);
        sdrBar.setBounds(850, 201, 500, 20);
        sdrBar.setFormatString("SDR: %1.2f");
        
        sdrBar.setFontSize(14);
        sdrBar.setGreenLevel(0.45f);
        sdrBar.setYellowLevel(0.4f);
        
       incidentAngleChart = new HistogramChartControl("Incident Angle", "Element Count", 300, 50, 500, 150);
       incidentAngleChart.setXAxisFormat("##");
       incidentAngleChart.setVisible(false);
       
       sdrChart = new HistogramChartControl("SDR Value", "Element Count", 850, 50, 500, 150);
       sdrChart.setXAxisFormat(".##");
       sdrChart.setVisible(false);

        transFuncDisplay.setBounds(550, 2, 600, 38);
        
        RenderList steeringTransformGroup = new RenderList();
        steeringTransform = new TransformationAdapter(steeringTransformGroup);
        steeringTransform.rotate(new Vector3f(0f, 0f, 1f), 180);
        steeringTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
        
        RenderList boreTransformGroup = new RenderList();
        mrBoreTransform = new TransformationAdapter(boreTransformGroup);
//        mrBoreTransform.rotate(new Vector3f(1f, 0f, 0f), 180);
//        mrBoreTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
                
        frameOffsetTransform = new TransformationAdapter(stereotacticFrame);
        frameOffsetTransform.rotate(new Vector3f(0, 0, 1), 180);
        frameOffsetTransform.translate(new Vector3f(0f, -40f, -75f)); // Plausible, if not accurate frame position
        
        frameTransform = new TransformationAdapter(frameOffsetTransform);
        
        
        RenderList globalTransformList = new RenderList();        
        TransformationAdapter globalTransform = new TransformationAdapter(globalTransformList);
        globalTransform.rotate(new Vector3f(0f, 0f, 1f), 180);
        globalTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
//        globalTransform.translate(new Vector3f(0f, 0f, 150f));
        
//                glTranslatef(0f, 0f, 300f);
//                glRotatef(180f, 0f, 0f, 1f);
//                //glRotatef(3f, 0f, 0f, 1f);
//                //glRotatef(-5f, 1f, 0f, 0f);
//                glTranslated(0.0, -10f, -220.00);  /////HACK   manual registration for specific frame model
//
//                // final translation for targetting
//                glTranslatef(skull.getXpos(), skull.getYpos(), skull.getZpos());
        
//                glTranslatef(0f, 0f, 300f);
//                glRotatef(180f, 0f, 0f, 1f);
//                //glRotatef(3f, 0f, 0f, 1f);
//                //glRotatef(-5f, 1f, 0f, 0f);
//                glTranslated(0.0, -10f, -220.00);  /////HACK   manual registration for specific frame model
        
        boreTransformGroup.add(mrBore2);
        boreTransformGroup.add(mrHousing);
        boreTransformGroup.add(mrPedestal);
        boreTransformGroup.add(mrBoreGroup);
        
//        globalTransformList.add(transducerModel);
        steeringTransformGroup.add(transducerModel);
        steeringTransformGroup.add(mrBoreTransform);
        
        globalTransformList.add(transRayTracer);
        
        background.setClearColor(0.22f, 0.25f, 0.30f, 1f);
        background.setIs2d(true);

//        background.addChild(new DirtyFollower(mainLayer));
        
//        mainLayer.setClearColor(0.22f, 0.25f, 0.30f, 1f);
        mainLayer.setClearColor(0f, 0f, 0f, 0f);
         
        mainLayer.addChild(trackball);
        mainLayer.addChild(frameTransform);
//        mainLayer.addChild(mrBoreTransform);
        mainLayer.addChild(globalTransform);
        mainLayer.addChild(steeringTransform);
        mainLayer.addChild(canvas);
        mainLayer.addChild(transFuncDisplayProxy); // this will trigger updates to mainLayer when the overlay widget is dirty
        mainLayer.addChild(new RenderableAdapter(transducerModel, "renderFocalSpot"));
        mainLayer.addChild(new CrossHair(this.trackball));
        
        CrossHair steeringCrossHair = new CrossHair(this.trackball);
        steeringCrossHair.setOffset(this.currentSteering.getLocation());
        steeringCrossHair.setStyle(1);
        
        mainLayer.addChild(steeringCrossHair);
        
        // add animation renderables
        mainLayer.addChild(zoomAnimator);
        mainLayer.addChild(orientAnimator);
                
        
        // The RenderableAdapter allows one Renderable object to render into a different layer
        // with a different method name that we can specify at runtime. The canvas object renders
        // the volume rendering into the main layer with the standard render() method and the
        // demographics into the overlay layer.
        overlay.setIs2d(true);
        overlay.addChild(new RenderableAdapter(canvas, "renderDemographics"));
        
        overlay.addChild(mprLayout);
        overlay.addChild(canvas1);
        overlay.addChild(canvas2);
        overlay.addChild(canvas3);
        overlay.addChild(flyout1);
        overlay.addChild(flyout2);
        overlay.addChild(flyout3);
        overlay.addChild(activeElementsBar);
        overlay.addChild(sdrBar);
        overlay.addChild(statusBar);
        overlay.addChild(transFuncDisplay);
        
        overlay.addChild(incidentAngleChart);
        overlay.addChild(sdrChart);
        
        currentTarget.setCommand("currentTargetPoint");
        currentTarget.setPropertyPrefix("Model.Attribute");
        model.addObserver(currentTarget);
        
        currentSteering.setCommand("currentTargetSteering");
        currentSteering.setPropertyPrefix("Model.Attribute");
        model.addObserver(currentSteering);
        
        // Send events to the controller
        canvas.addActionListener(controller);
        canvas.setCommand("currentTargetPoint");
        canvas.setTrackball(trackball);
        canvas.setTransferFunction(transFuncDisplay.getTransferFunction());
        
        canvas1.addActionListener(controller);
        canvas2.addActionListener(controller);
        canvas3.addActionListener(controller);
        canvas1.setCommand("currentTargetPoint");
        canvas2.setCommand("currentTargetPoint");
        canvas3.setCommand("currentTargetPoint");
        
        // Bind to property change update notifications
        canvas.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas2.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas3.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(canvas);
        model.addObserver(canvas1);
        model.addObserver(canvas2);
        model.addObserver(canvas3);
        
        ImageLabel logoLabel = new ImageLabel("images/KranionMed.png", 10, 0, 500, 200);
        //ImageLabel logoLabel = new ImageLabel("images/KranionSm.png", 110, 0, 500, 200);
        //logoLabel.setBounds(0, 0, logoLabel.getBounds().width/1.8f, logoLabel.getBounds().height/1.8f);
        //logoLabel.setVisible(false);
        overlay.addChild(logoLabel);
        
        // little oriented human figure in lower left corner
        CoordinateWidget widget = new CoordinateWidget();
        widget.setTrackball(trackball);
        overlay.addChild(widget);
      
        background.setSize(Display.getWidth(), Display.getHeight());
        overlay.setSize(Display.getWidth(), Display.getHeight());
        mainLayer.setSize(Display.getWidth(), Display.getHeight());
        
        scene.addChild(background);
        scene.addChild(mainLayer);
        scene.addChild(overlay);
        scene.addChild(transition); // must be last to work properly

    }
        
    // sets flag to do an animated blend transition at next frame update
    public void setDoTransition(boolean doTransition) {
        setDoTransition(doTransition, 0.5f);
    }
    
    public void setDoTransition(boolean doTransition, float time) {
        this.doTransition = doTransition;
        this.transitionTime = time;
    }
    
    public void setDisplayCTimage(ImageVolume image) {
        if (image == null) return;
        
        float windowWidth;
        float windowCenter;
        
        try {
            windowWidth = (Float) image.getAttribute("WindowWidth");
            windowCenter = (Float) image.getAttribute("WindowCenter");
        }
        catch(NullPointerException e) {
            windowWidth = 3000f;
            windowCenter = 1000f;
        }

        float rescaleSlope = (Float) image.getAttribute("RescaleSlope");
        float rescaleIntercept = (Float) image.getAttribute("RescaleIntercept");

        this.center = (int)(windowCenter*rescaleSlope - rescaleIntercept);
        this.window = (int)(windowWidth*rescaleSlope - rescaleIntercept);
        this.ct_threshold = (int) ((1800f - rescaleIntercept) / rescaleSlope);

        if (model != null) {
            model.setAttribute("CTwindow", (float)window);
            model.setAttribute("CTcenter", (float)center);
            model.setAttribute("CTthresh", (float)ct_threshold);
       }
        
        canvas.setCTImage(image);
        canvas.setCenterWindow(center, window);
        canvas.setOrientation(0);
        canvas.setCTThreshold(ct_threshold);

        System.out.println("MAIN: setImage #2");
        canvas1.setCTImage(image);
        canvas1.setCenterWindow((int) center, (int) window);
        canvas1.setOrientation(0);
        canvas1.setCTThreshold(ct_threshold);

        canvas2.setCTImage(image);
        canvas2.setCenterWindow((int) center, (int) window);
        canvas2.setOrientation(1);
        canvas2.setCTThreshold(ct_threshold);

        canvas3.setCTImage(image);
        canvas3.setCenterWindow((int) center, (int) window);
        canvas3.setOrientation(2);
        canvas3.setCTThreshold(ct_threshold);

        transRayTracer.setImage(image);
        ctHistogram.setImage(image);
    }
    
    public void setDisplayMRimage(ImageVolume image) {
        
        if (image == null) return;
        
        float windowWidth = 4095;
        float windowCenter = 1024;
        try {
            windowWidth = (Float) image.getAttribute("WindowWidth");
            windowCenter = (Float) image.getAttribute("WindowCenter");
        }
        catch(Exception e) {
              e.printStackTrace();
        }
        
        //            float rescaleSlope = (Float)mrImage.getAttribute("RescaleSlope");
        //            float rescaleIntercept = (Float)mrImage.getAttribute("RescaleIntercept");
        this.mr_center = (int) windowCenter;
        this.mr_window = (int) windowWidth;
        
        if (model != null) {
            model.setAttribute("MRwindow", (float)mr_window);
            model.setAttribute("MRcenter", (float)mr_center);
            model.setAttribute("MRthresh", 125f);
        }

        canvas.setMRImage(image);
        canvas.setMRCenterWindow((int) windowCenter, (int) windowWidth);
        canvas.setOrientation(0);
        canvas.setMRThreshold(450f);

        canvas1.setMRImage(image);
        canvas1.setMRCenterWindow((int) windowCenter, (int) windowWidth);
        canvas1.setOrientation(0);
        canvas1.setMRThreshold(450f);

        canvas2.setMRImage(image);
        canvas2.setMRCenterWindow((int) windowCenter, (int) windowWidth);
        canvas2.setOrientation(1);
        canvas2.setMRThreshold(450f);

        canvas3.setMRImage(image);
        canvas3.setMRCenterWindow((int) windowCenter, (int) windowWidth);
        canvas3.setOrientation(2);
        canvas3.setMRThreshold(450f);
    }

    private void saveScene() {
        try {
            File selectedFile;
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(new String("Choose scene file name to save..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
            }
            else {
                return;
            }
            
            FileWriter saveFile = new FileWriter(selectedFile);
            PrintWriter writer = new PrintWriter(saveFile);
            
            Quaternion orient = ((Quaternion)model.getCtImage().getAttribute("ImageOrientationQ"));
            Vector3f trans = (Vector3f)model.getCtImage().getAttribute("ImageTranslation");
            
            writer.println(orient.x);
            writer.println(orient.y);
            writer.println(orient.z);
            writer.println(orient.w);
            
            writer.println(trans.x);
            writer.println(trans.y);
            writer.println(trans.z);
            
            try {
                orient = ((Quaternion)model.getMrImage(0).getAttribute("ImageOrientationQ"));
            }
            catch(NullPointerException e) {
                orient = new Quaternion().setIdentity();
            }
            
            try {
                trans = (Vector3f)model.getMrImage(0).getAttribute("ImageTranslation");
            }
            catch(NullPointerException e) {
                trans = new Vector3f();
            }
            
            writer.println(orient.x);
            writer.println(orient.y);
            writer.println(orient.z);
            writer.println(orient.w);
            
            writer.println(trans.x);
            writer.println(trans.y);
            writer.println(trans.z);
            
            writer.println(this.center);
            writer.println(this.window);
            writer.println(this.mr_center);
            writer.println(this.mr_window);

            //Close writer
            writer.close();
            saveFile.close();
        }
        catch (Exception e) {
            System.out.println("Error writing scene file.");
        }
        
    }
    
    private void loadScene() {
        try {
            
            File selectedFile;
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(new String("Choose scene file..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
            }
            else {
                return;
            }

            String sCurrentLine;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(selectedFile));
            
            Quaternion orient = new Quaternion();
            Vector3f trans = new Vector3f();
            
            orient.x = Float.parseFloat(bufferedReader.readLine());
            orient.y = Float.parseFloat(bufferedReader.readLine());
            orient.z = Float.parseFloat(bufferedReader.readLine());
            orient.w = Float.parseFloat(bufferedReader.readLine());
            
            trans.x = Float.parseFloat(bufferedReader.readLine());
            trans.y = Float.parseFloat(bufferedReader.readLine());
            trans.z = Float.parseFloat(bufferedReader.readLine());
            
            model.getCtImage().setAttribute("ImageOrientationQ", orient);
            model.getCtImage().setAttribute("ImageTranslation", trans);
            
            orient = new Quaternion();
            trans = new Vector3f();
            
            orient.x = Float.parseFloat(bufferedReader.readLine());
            orient.y = Float.parseFloat(bufferedReader.readLine());
            orient.z = Float.parseFloat(bufferedReader.readLine());
            orient.w = Float.parseFloat(bufferedReader.readLine());
            
            trans.x = Float.parseFloat(bufferedReader.readLine());
            trans.y = Float.parseFloat(bufferedReader.readLine());
            trans.z = Float.parseFloat(bufferedReader.readLine());
            
            try {
                model.getMrImage(0).setAttribute("ImageOrientationQ", orient);
                model.getMrImage(0).setAttribute("ImageTranslation", trans);
            }
            catch(NullPointerException e) {
                
            }
            
            this.center = Integer.parseInt(bufferedReader.readLine());
            this.window = Integer.parseInt(bufferedReader.readLine());
            this.mr_center = Integer.parseInt(bufferedReader.readLine());
            this.mr_window = Integer.parseInt(bufferedReader.readLine());
            
            if (model != null) {
                model.setAttribute("MRcenter", (float)mr_center);
                model.setAttribute("MRwindow", (float)mr_window);
                model.setAttribute("CTcenter", (float)center);
                model.setAttribute("CTwindow", (float)window);
            }
            
            canvas.setCenterWindow(center, window);
            canvas.setMRCenterWindow(mr_center, mr_window);
            canvas1.setCenterWindow(center, window);
            canvas1.setMRCenterWindow(mr_center, mr_window);
            canvas2.setCenterWindow(center, window);
            canvas2.setMRCenterWindow(mr_center, mr_window);
            canvas3.setCenterWindow(center, window);
            canvas3.setMRCenterWindow(mr_center, mr_window);

            //Close reader
            bufferedReader.close();
        }
        catch (Exception e) {
            System.out.println("Error reading scene file.");
        }
        
    }

    private void showThermometry(int sonicationIndex) {
        Vector3f steering = this.currentSteering.getLocation();//(Vector3f)model.getAttribute("currentTargetSteering");
        if (steering == null) {
            steering = new Vector3f(0, 0, 0);
        }
        Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
        Vector3f spotPosition = Vector3f.add(naturalFocusPosition, steering, null);
        
        ImageVolume image = model.getSonication(sonicationIndex).getThermometryPhase();
        Integer currentFrame = (Integer)model.getSonication(sonicationIndex).getAttribute("currentFrame");
        if (currentFrame == null) {
            currentFrame = new Integer(0);
        }
        
        canvas.setShowPressure(false);
        
        if (model.getAttribute("showThermometry") != null) {
            boolean bShow = (Boolean)model.getAttribute("showThermometry");
            canvas.setShowThermometry(bShow);
        }
        else {
            canvas.setShowThermometry(false);            
        }

        System.out.println("Spot position " + spotPosition);
        
        float[] imageLoc = (float[])image.getAttribute("ImagePosition");
        System.out.println("Image position = " + imageLoc[0] + ", " + imageLoc[1] + ", " + imageLoc[2]);
                
        
//TODO: need to understand this better. The image position reported by loader doesn't seem to land the center
//      on the spot position for some reason, even though that is what should happen. So setting the image center
//      to the origin and translating to the spot location for now. Looks right but doesn't feel right.
//        image.setAttribute("ImagePosition", new float[3]);
//        image.setAttribute("ImageTranslation", new Vector3f(-spotPosition.x, -spotPosition.y, -spotPosition.z));
        canvas.setCurrentOverlayFrame(currentFrame);
        ImageVolumeUtil.releaseTexture(image);
        canvas.setOverlayImage(image);
        
//        try {
//            image.setAttribute("ImageTranslation", new Vector3f((Vector3f)model.getMrImage(0).getAttribute("ImageTranslation")));
//        }
//        catch(NullPointerException e) {
//
//            image.setAttribute("ImageTranslation", new Vector3f());
//        }
                model.setAttribute("currentSceneOrienation", (Quaternion)image.getAttribute("ImageOrientationQ"));

    }
    
    private void zeroImageTranslations() {
        try {
            model.getCtImage().setAttribute("ImageTranslation", new Vector3f());
        }
        catch(Exception e) {}
        
        for (int i=0; i<model.getMrImageCount(); i++) {
            try {
                model.getMrImage(i).setAttribute("ImageTranslation", new Vector3f());
            }
            catch(Exception e) {}
        }
    }
    
    private void updateTargetAndSteering() {
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f(0, 0, 0);
            }
            this.transRayTracer.setTargetSteering(steering.x, -steering.y, -steering.z);
            
            Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
            Vector3f spotPosition = Vector3f.add(naturalFocusPosition, steering, null);
            
            canvas.setTextureRotatation(spotPosition, trackball);
            
            this.transRayTracer.setTextureRotatation(naturalFocusPosition, trackball);
            
            //zeroImageTranslations();
    }
    
    private void updatePressureCalc() {
        if (canvas.getShowPressure()) {
                       
            this.transRayTracer.calcPressureEnvelope();

            canvas.setShowPressure(true);
            canvas.setOverlayImage(transRayTracer.getEnvelopeImage());

            canvas1.setOverlayImage(null);
            canvas2.setOverlayImage(null);
            canvas3.setOverlayImage(null);
        }
    }
    
    private void updateFromModel() {

        
        try {
            this.showRayTracer = (Boolean)model.getAttribute("showRayTracer");
            
            if (!this.showRayTracer) {
                activeElementsBar.setValue(-1f);
                sdrBar.setValue(-1f);
                incidentAngleChart.setVisible(false);
                sdrChart.setVisible(false);
            }
            else {
               incidentAngleChart.setVisible(true);
               sdrChart.setVisible(true);                
            }
        }
        catch (NullPointerException e) {
            this.showRayTracer = false;
        }
        
        try {
            this.doClip = (Boolean)model.getAttribute("doClip");
        }
        catch(NullPointerException e) {
            this.doClip = false;
        }
        
        try {
            this.doMRI = (Boolean)model.getAttribute("doMRI");
        }
        catch(NullPointerException e) {
            this.doMRI = false;
        }

        try {
            this.doFrame = (Boolean)model.getAttribute("doFrame");
        }
        catch(NullPointerException e) {
            this.doFrame = false;
        }
        
        try {
            this.transRayTracer.setBoneSpeed((Float)model.getAttribute("boneSOS"));
        }
        catch(NullPointerException e) {
        }  
        
        try {
            this.transRayTracer.setBoneRefractionSpeed((Float)model.getAttribute("boneRefractionSOS"));
        }
        catch(NullPointerException e) {
        }
        
        try {
            mr_center = ((Float)model.getAttribute("MRcenter")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            boolean old = canvas.getShowThermometry();
            boolean bShow = (Boolean)model.getAttribute("showThermometry");
            canvas.setShowThermometry(bShow);
            
            if (old != bShow) {
                this.setDoTransition(true);;
            }
        }
        catch(NullPointerException e) {
        }
        
        try {
            mr_window = ((Float)model.getAttribute("MRwindow")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            mr_threshold = ((Float)model.getAttribute("MRthresh")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            center = ((Float)model.getAttribute("CTcenter")).intValue();
        }
            catch(NullPointerException e) {
        }
        
        try {
            window = ((Float)model.getAttribute("CTwindow")).intValue();
        }
            catch(NullPointerException e) {
        }
       
        try {
            this.canvas.setForegroundVolumeSlices(((Float)model.getAttribute("foregroundVolumeSlices")).intValue());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        catch(NullPointerException e) {
        }        
    }
    
    @Override
    public void render() {
        
//        setIsDirty(false);

        updateFromModel();
        
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
        gluLookAt(0.0f, 0.0f, cameraZ, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);

        // camera dolly in/out
        glTranslatef(0.0f, 0.0f, dolly.getValue());

        // clip plan through the focal point normal to camera view
        DoubleBuffer eqnBuf = BufferUtils.createDoubleBuffer(4);
        eqnBuf.put(0.0f).put(0.0f).put(1.0f).put(8f);
        eqnBuf.flip();
        glClipPlane(GL_CLIP_PLANE0, eqnBuf);

        DoubleBuffer eqnBuf2 = BufferUtils.createDoubleBuffer(4);
        eqnBuf2.put(0.0f).put(0.0f).put(-1.0f).put(8f);
        eqnBuf2.flip();
        glClipPlane(GL_CLIP_PLANE1, eqnBuf2);

//        trackball.render();

        mrBoreTransform.setTranslation(-currentTarget.getXpos(), currentTarget.getYpos(), 0f);
        
        frameTransform.setTranslation(
                -currentTarget.getXpos()-currentSteering.getXpos(),
                -currentTarget.getYpos()-currentSteering.getYpos(),
                -currentTarget.getZpos()-currentSteering.getYpos());
        
        steeringTransform.setTranslation( -currentSteering.getXpos(), -currentSteering.getYpos(), -currentSteering.getZpos());

        Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(),
                                                     currentTarget.getYpos(),
                                                     currentTarget.getZpos());
        
        Vector3f spotPosition = new Vector3f(currentTarget.getXpos()+currentSteering.getXpos(),
                                             currentTarget.getYpos()+currentSteering.getYpos(),
                                             currentTarget.getZpos()+currentSteering.getZpos());
        
        // render oblique MR slice
        if (doMRI) {

            canvas.setVisible(true);
            
            // These could all eventuall be done as property notifications
            canvas.setCenterWindow(center, window);
            canvas1.setCenterWindow(center, window);
            canvas2.setCenterWindow(center, window);
            canvas3.setCenterWindow(center, window);

            canvas.setMRCenterWindow(mr_center, mr_window);
            canvas1.setMRCenterWindow(mr_center, mr_window);
            canvas2.setMRCenterWindow(mr_center, mr_window);
            canvas3.setMRCenterWindow(mr_center, mr_window);

            canvas.setCTThreshold(ct_threshold);
            canvas.setMRThreshold(mr_threshold);

            // don't need this now. canvas.update() will get changes from model
//            canvas.setTextureRotatation(CofR, trackball);
        } else {
            canvas.setVisible(false);
        }

        // render frame
        if (doFrame) {
            stereotacticFrame.setVisible(true);

            stereotacticFrame.getShader().start();

            Vector3f loc = Vector3f.sub(currentTarget.getLocation(), this.frameOffsetTransform.getTranslation(), null);
            
            int uniformLoc = glGetUniformLocation(stereotacticFrame.getShader().getShaderProgramID(), "offset");
            glUniform3f(uniformLoc, loc.x, loc.y, loc.z); // 300 - 220 = 80

            uniformLoc = glGetUniformLocation(stereotacticFrame.getShader().getShaderProgramID(), "transducerAngle");
            glUniform1f(uniformLoc, (float) (transducerTilt / 180f * Math.PI));

            stereotacticFrame.getShader().stop();

            if (doClip) {
                stereotacticFrame.setClipped(false);
            } else {
                stereotacticFrame.setClipped(false);
            }
        } else {
            stereotacticFrame.setVisible(false);
        }

        if (showScanner) {
            if (doClip) {
                mrBore2.setClipped(true);

                mrBore2.setVisible(true);
                mrHousing.setVisible(false);
                mrPedestal.setVisible(false);

                mrBore2.setClipColor(0.6f, 0.6f, 0.6f, 1f);
                mrHousing.setClipColor(0.4f, 0.4f, 0.4f, 1f);

                mrBore2.setTrackball(trackball);
                mrBore2.setDolly(cameraZ, dolly.getValue());
            } else {
                mrBore2.setClipped(false);

                mrBore2.setVisible(true);
                mrHousing.setVisible(true);
                mrPedestal.setVisible(true);
            }
        } else {
            mrBore2.setVisible(false);
            mrHousing.setVisible(false);
            mrPedestal.setVisible(false);
        }

        // select shaded or wireframe rendering for the transducer
//        if (drawingStyle == 0) {
//            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//        } else {
//            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
//        }

        transducerModel.setVisible(true);
        if (doClip) {
            transducerModel.setClipped(true);
            transducerModel.setTrackball(trackball);
            transducerModel.setDolly(cameraZ, dolly.getValue());
        } else {
            transducerModel.setClipped(false);
        }

        transducerModel.setTransducerXTilt(transducerTilt);

        if (showRayTracer) {
            transRayTracer.setVisible(true);
            transRayTracer.setTextureRotatation(naturalFocusPosition, trackball);
            transRayTracer.setTransducerTilt(-transducerTilt);

            activeElementsBar.setValue(transRayTracer.getActiveElementCount());
            sdrBar.setValue(transRayTracer.getSDR());

        } else {
            transRayTracer.setVisible(false);
        }

        if (!showScanner) {
            if (doClip) {
                mrBoreGroup.setClipped(true);
                mrBoreGroup.setTrackball(trackball);
                mrBoreGroup.setDolly(cameraZ, dolly.getValue());
            } else {
                mrBoreGroup.setClipped(false);
            }
        }

        if (doMRI) {
            canvas.setVisible(true);
        }

//        canvas1.setTextureRotatation(spotPosition, trackball);
//        canvas2.setTextureRotatation(spotPosition, trackball);
//        canvas3.setTextureRotatation(spotPosition, trackball);

        if (canvas.getVolumeRender() == true) {
            transFuncDisplay.setVisible(true);
        } else {
            transFuncDisplay.setVisible(false);
        }
        
        if (doTransition) {
            System.out.println("Do transition");
            transition.doTransition(transitionTime);
            doTransition = false;
        }

        scene.render();

    }

    private static boolean mouseButton1Drag = false;
    private boolean OnMouse(int x, int y, boolean button1down, boolean button2down, int dwheel) {
        if (dwheel != 0) {
            scene.setIsDirty(true);
            dolly.incrementValue(-dwheel/5f); // zoom with mouse wheel
        }
        if (!scene.OnMouse(x, y, button1down, button2down, dwheel)) {
            
            // Mouse dragged
            if (mouseButton1Drag == true && Mouse.isButtonDown(0)) {
//                System.out.println("*** Mouse dragged");
                if (orientAnimator != null) {
                    orientAnimator.cancelAnimation();
                }

                if (currentMouseMode == mouseMode.SCENE_ROTATE) {
                    trackball.mouseDragged(x, y);
                    return true;
                }
                
                if (currentMouseMode == mouseMode.SKULL_ROTATE || currentMouseMode == mouseMode.HEAD_ROTATE) {
                    
                    if (model.getCtImage() == null) return false;
                    
                    registerBallCT.mouseDragged(x, y);
                    Quaternion mrQnow = new Quaternion(registerBallCT.getCurrent());
                    Quaternion.mul(trackball.getCurrent().negate(null), mrQnow, mrQnow);
                    model.getCtImage().setAttribute("ImageOrientationQ", mrQnow.negate(null));
                    this.setIsDirty(true);

                    if (currentMouseMode == mouseMode.HEAD_ROTATE) {
                        try {
                            mrQnow = new Quaternion().setIdentity();

                            for (int i = 0; i < model.getMrImageCount(); i++) {
                                Trackball registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                                if (registerBall == null) {
                                    registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                                    model.getMrImage(i).setAttribute("registerBall", registerBall);
                                }
                                registerBall.mouseDragged(x, y);
                                mrQnow = new Quaternion(registerBall.getCurrent());
                                Quaternion.mul(trackball.getCurrent().negate(null), mrQnow, mrQnow);
                                model.getMrImage(i).setAttribute("ImageOrientationQ", mrQnow.negate(null));

                            }

                            // Need to fix the translation of the MR images since these rotations are about the image volume centers
                            Vector3f mrtrans = (Vector3f) model.getMrImage(0).getAttribute("startMrImageTranslate");
                            if (mrtrans == null) {
                                mrtrans = new Vector3f();
                            }
                            Vector4f mrtrans4f = new Vector4f(mrtrans.x, mrtrans.y, mrtrans.z, 1f);
                            
                            float[] mrImagePos = (float[]) model.getMrImage(0).getAttribute("ImagePosition");
                            Vector4f mrPos4f = new Vector4f(mrImagePos[0], mrImagePos[1], mrImagePos[2], 1f);

                            Vector3f cttrans = startCtImageTranslate;
                            if (cttrans == null) {
                                cttrans = new Vector3f();
                            }
                            Vector4f cttrans4f = new Vector4f(cttrans.x, cttrans.y, cttrans.z, 1f);

                            float[] ctImagePos = (float[]) model.getCtImage().getAttribute("ImagePosition");
                            Vector4f ctPos4f = new Vector4f(ctImagePos[0], ctImagePos[1], ctImagePos[2], 1f);

                            Vector4f ctRotVec = Vector4f.sub(Vector4f.add(cttrans4f, ctPos4f, null), Vector4f.add(mrtrans4f, mrPos4f, null), null);
                            //    Vector4f ctRotVec = Vector4f.sub(cttrans4f, mrtrans4f, null);
                            //Vector4f ctRotVec = Vector4f.add(cttrans4f, ctPos4f, null);
                            ctRotVec.w = 1f;

                            Quaternion ctQnow = new Quaternion(this.registerBallCT.getCurrent());
                            Quaternion.mul(trackball.getCurrent().negate(null), ctQnow, ctQnow);

                            Matrix4f mat4 = Trackball.toMatrix4f(ctQnow);
                            Matrix4f.transform(mat4, ctRotVec, ctRotVec);

                            //    Vector4f ctCoordFrame = Vector4f.add(ctRotVec, mrPos4f, null);
                            Vector4f ctCoordFrame = ctRotVec;
                            Vector4f.add(ctCoordFrame, mrtrans4f, ctCoordFrame);
                            Vector4f.add(ctCoordFrame, mrPos4f, ctCoordFrame);
                            Vector4f.sub(ctCoordFrame, ctPos4f, ctCoordFrame);

                            //        ctCoordFrame = Vector4f.sub(ctCoordFrame, ctPos4f, null);
                            ctCoordFrame.w = 1f;

                            Vector3f translate = new Vector3f(ctCoordFrame);

                            model.getCtImage().setAttribute("ImageTranslation", translate);

                            this.setIsDirty(true);

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    return true;
                }                
                else if (currentMouseMode == mouseMode.SKULL_TRANSLATE || currentMouseMode == mouseMode.HEAD_TRANSLATE) {
                    System.out.println("*** Skull translate");
                    
                    if (model.getCtImage() == null) return false;
                    
                    // image translation in the plane of the screen
                    Quaternion orient = trackball.getCurrent().negate(null);
                    Matrix4f mat4 = Trackball.toMatrix4f(orient);
                    Vector4f offset = new Vector4f((x - mouseStartX) / 5f, -(y - mouseStartY) / 5f, 0f, 0f);
                    System.out.println("offset = " + offset);
                    Matrix4f.transform(mat4, offset, offset);
                    Vector3f translate = new Vector3f(offset);

                    model.getCtImage().setAttribute("ImageTranslation", Vector3f.add(startCtImageTranslate, translate.negate(null), null));
                    this.setIsDirty(true);

                    if (currentMouseMode == mouseMode.HEAD_TRANSLATE) {
                        System.out.println("*** Head translate");
                        try {
                            for (int i = 0; i < model.getMrImageCount(); i++) {
                                Vector3f startMrImageTranslate = (Vector3f) model.getMrImage(i).getAttribute("startMrImageTranslate");
                                if (startMrImageTranslate == null) {
                                    startMrImageTranslate = new Vector3f();
                                }
                                System.out.println("startMRImageTranslate = " + startMrImageTranslate);
                                model.getMrImage(i).setAttribute("ImageTranslation", Vector3f.add(startMrImageTranslate, translate.negate(null), null)); //TEMP CHANGE
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        this.setIsDirty(true);
                    }
                    return true;
                }
            }
            // Mouse pressed, init drag
            else if (button1down && !mouseButton1Drag) {
//                System.out.println("*** Mouse pressed, drag init");
                mouseButton1Drag = true;
                
                this.mouseStartX = x;
                this.mouseStartY = y;
                
                try {
                    startCtImageTranslate = new Vector3f((Vector3f)model.getCtImage().getAttribute("ImageTranslation"));
                    if (startCtImageTranslate == null) {
                        startCtImageTranslate = new Vector3f();
                    }
                }
                catch(NullPointerException e) {
                    startCtImageTranslate = new Vector3f(0, 0, 0);
                }
                
                for (int i = 0; i < model.getMrImageCount(); i++) {
                    try {
                        Vector3f startMrImageTranslate = (Vector3f) model.getMrImage(i).getAttribute("ImageTranslation"); //TEMP CHANGE
                        if (startMrImageTranslate == null) {
                            startMrImageTranslate = new Vector3f();
                        }
                        model.getMrImage(i).setAttribute("startMrImageTranslate", new Vector3f(startMrImageTranslate));
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        model.getMrImage(i).setAttribute("startMrImageTranslate", new Vector3f());
                    }
                }
                

                if (currentMouseMode == mouseMode.SCENE_ROTATE) {
                    trackball.mousePressed(x, y);
                    return true;
                }
                else if (this.currentMouseMode == mouseMode.HEAD_ROTATE
                        || this.currentMouseMode == mouseMode.SKULL_ROTATE) {
                    
                    
                    if (model.getCtImage() == null) return false;

                    ctQstart = new Quaternion((Quaternion) model.getCtImage().getAttribute("ImageOrientationQ")).negate(null);
                    Quaternion.mul(trackball.getCurrent(), ctQstart, ctQstart);
                    registerBallCT.setCurrent(ctQstart);
                    registerBallCT.mousePressed(x, y);

                    if (this.currentMouseMode == mouseMode.HEAD_ROTATE) {
                        for (int i = 0; i < model.getMrImageCount(); i++) {
                            Quaternion mrQstart = ((Quaternion) model.getMrImage(i).getAttribute("ImageOrientationQ")).negate(null);

                            Trackball registerBall = null;
                            try {
                                registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                                if (registerBall == null) {
                                    registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                                    model.getMrImage(i).setAttribute("registerBall", registerBall);                                    
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }

                            Quaternion.mul(trackball.getCurrent(), mrQstart, mrQstart);
                            registerBall.setCurrent(new Quaternion(mrQstart));
                            registerBall.mousePressed(x, y);
                        }
                    }
                }
                return true;
            }
            // Mouse released, end drag
            else if (!button1down && mouseButton1Drag) {
//                System.out.println("*** Mouse released, drag end");
                mouseButton1Drag = false;
                trackball.mouseReleased(x, y);
                registerBallCT.mouseReleased(x, y);

                //for each MR image in the model
                for (int i = 0; i < model.getMrImageCount(); i++) {
                    Trackball registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                    if (registerBall == null) {
                        registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                        model.getMrImage(i).setAttribute("registerBall", registerBall);
                    }
                    registerBall.mouseReleased(x, y);
                }
                
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void doLayout() {
        trackball.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
        registerBallCT.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
        try {
            for (int i=0; i<model.getMrImageCount(); i++) {
                Trackball registerBall = (Trackball)model.getMrImage(i).getAttribute("registerBall"); //TEMP CHANGE
                if (registerBall == null) {
                   registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f); 
                }
                registerBall.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                model.getMrImage(i).setAttribute("registerBall", registerBall);
            }
        }
        catch(NullPointerException e) {
        }
        
        this.statusBar.setBounds(Display.getWidth() / 2 - 300, Display.getHeight() / 2 - 15, 600, 30);

        scene.doLayout();
        
    }

    @Override
    public void release() {
        scene.release();
    }
    
    @Override
    public boolean getIsDirty() {
        // empty updateEventQueue
        ////////////////////////////
        updateEventQueue.handleEvents(this);

        return scene.getIsDirty();
    }
    
    @Override
    public void setIsDirty(boolean dirty) {
        scene.setIsDirty(dirty);
    }
    
    @Override
    public void update(Observable o, Object arg) {
        
        // If updates are called from a different thread than the
        // main thread, queue them for later processing on the main thread.
        if (myThread != Thread.currentThread()) {
            updateEventQueue.push(o, arg);
            return;
        }
// To display property change notifications for debugging        
//        if (o != null) {
//            System.out.print("----DefaultView update: " + o.toString());
//        }
        
        if (arg != null && arg instanceof PropertyChangeEvent) {
            PropertyChangeEvent event = (PropertyChangeEvent)arg;
//            System.out.print(" Property Change: " + ((PropertyChangeEvent)arg).getPropertyName());
//            System.out.println();
            
            switch(this.getFilteredPropertyName(event)) {
                case "currentSceneOrienation":
                    Quaternion orient = (Quaternion)event.getNewValue();
                    orientAnimator.set(trackball.getCurrent(), orient, 1.5f);
                    orientAnimator.setTrackball(trackball);
                    return;
                case "currentTargetPoint":
                case "currentTargetSteering":
                case "boneSOS":
                case "boneRefractionSOS":
                    updateTargetAndSteering();
                    updatePressureCalc();
                    this.setIsDirty(true);
                    return;
                case "doFrame":
                case "doMRI":
                case "doClip":
                case "showRayTracer":
                    setDoTransition(true);;
                    return;
                case "currentMRSeries":
                    this.setDisplayMRimage(model.getMrImage( ((Integer)model.getAttribute("currentMRSeries")).intValue()));
                    setDoTransition(true);;
                    break;
                case "currentSonication":
                    int sonicationIndex = (Integer)model.getAttribute("currentSonication");
                    if (sonicationIndex>=0) {
                        model.setAttribute("currentTargetPoint", new Vector3f(model.getSonication(sonicationIndex).getNaturalFocusLocation()));
                        model.setAttribute("currentTargetSteering", new Vector3f(model.getSonication(sonicationIndex).getFocusSteering()));
                        model.setAttribute("sonicationPower", String.format("%4.1f W", model.getSonication(sonicationIndex).getPower()));
                        model.setAttribute("sonicationDuration", String.format("%4.1f s", model.getSonication(sonicationIndex).getDuration()));
                        model.setAttribute("sonicationFrequency", String.format("%4.1f kHz", model.getSonication(sonicationIndex).getFrequency()/1000f));
                        
                        Vector3f t = Vector3f.add(model.getSonication(sonicationIndex).getFocusSteering(), model.getSonication(sonicationIndex).getNaturalFocusLocation(), null);
                        model.setAttribute("sonicationRLoc", String.format("%4.1f", -t.x));
                        model.setAttribute("sonicationALoc", String.format("%4.1f", -t.y));
                        model.setAttribute("sonicationSLoc", String.format("%4.1f", t.z));
                        updateThermometryDisplay(sonicationIndex, true);
                        updateTransducerModel(sonicationIndex);
                        setDoTransition(true);;
                    }
                    break;
                case "transducerXTilt":
                    this.transducerModel.setTransducerXTilt((float)event.getNewValue());
                    this.transducerTilt = (float)event.getNewValue();
                    break;
            }
            
            switch(event.getPropertyName()) {
                case "Model.CtImage":
                    setDisplayCTimage(model.getCtImage());
                    model.setAttribute("doMRI", true);
                    canvas.setVolumeRender(true);
                    canvas.setIsDirty(true);
                    ctHistogram.calculate();
                    transFuncDisplay.setHistogram(ctHistogram.getData());
                    ctHistogram.release();
                    setDoTransition(true);;
                    return;
                case "Model.MrImage[0]":
                    setDisplayMRimage(model.getMrImage(0));
                    return;
                case "rayCalc":
                    if (transRayTracer.getVisible()) {

                        Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                        barColor.x *= 2f;
                        barColor.y *= 2f;
                        barColor.z *= 2f;
                        barColor.w = 1f;

                        incidentAngleChart.newChart();
                        incidentAngleChart.addSeries("Frequency", transRayTracer.getIncidentAngles(), barColor, 35, 0f, 36f);
                    }
                    return;
                case "sdrCalc":
                    if (transRayTracer.getVisible()) {

                        Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                        barColor.x *= 2f;
                        barColor.y *= 2f;
                        barColor.z *= 2f;
                        barColor.w = 1f;

                        sdrChart.newChart();
                        sdrChart.addSeries("Frequency", transRayTracer.getSDRs(), barColor, 22, 0.0f, 1f);
                    }
                    return;
            }
            
            if (event.getPropertyName().startsWith("Model.MrImage[")) {
                if (Pattern.matches("Model\\.MrImage\\[\\d{1,2}\\]", event.getPropertyName())){                   
                    updateMRlist();
                }
            }
            else if (event.getPropertyName().startsWith("Model.Sonication[")) {
                if (Pattern.matches("Model\\.Sonication\\[\\d{1,2}\\]", event.getPropertyName())){                   
                    updateSonicationList();
                }                
            }
            
        }
        else if (arg != null && arg instanceof TransducerRayTracer) {
            if (transRayTracer.getVisible()) {
                
                Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                barColor.x *= 2f;
                barColor.y *= 2f;
                barColor.z *= 2f;
                barColor.w = 1f;
                
                incidentAngleChart.newChart();
                incidentAngleChart.addSeries("Frequency", transRayTracer.getIncidentAngles(), barColor, 35, 0f, 36f);
                incidentAngleChart.generateChart();
                
                sdrChart.newChart();
                sdrChart.addSeries("Frequency", transRayTracer.getSDRs(), barColor, 22, 0.0f, 1f);
                sdrChart.generateChart();
            }                    
        }
    }
    
    private void updateTransducerModel(int sonicationIndex) {
        try {
            String txdrGeomFileName = (String) model.getSonication(sonicationIndex).getAttribute("txdrGeomFileName");
            Vector3f tdXdir = (Vector3f) model.getSonication(sonicationIndex).getAttribute("txdrTiltXdir");
            Vector3f tdYdir = (Vector3f) model.getSonication(sonicationIndex).getAttribute("txdrTiltYdir");
            Vector3f tdZdir = Vector3f.cross(tdXdir, tdYdir, null);

            this.transducerModel.buildElements(new InsightecTxdrGeomReader(new File(txdrGeomFileName)));
            this.transducerModel.setTransducerTilt(tdXdir, tdYdir);
            this.transRayTracer.init(transducerModel);
            
            float tiltXAngleDeg = Vector3f.angle(new Vector3f(0, tdZdir.y, tdZdir.z), new Vector3f(0, 0, -1)) / ((float) Math.PI * 2f) * 360f;
            model.setAttribute("transducerXTilt", tiltXAngleDeg);
                    
    } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
    }
    
    private void updateThermometryDisplay(int sonicationIndex, boolean setToMax) {
        ImageVolume thermometry = model.getSonication(sonicationIndex).getThermometryPhase();
        float data[] = (float[])thermometry.getData();
        
        int cols = thermometry.getDimension(0).getSize();
        int rows = thermometry.getDimension(1).getSize();
        int timepoints = thermometry.getDimension(3).getSize();
        
        float phaseTime = thermometry.getDimension(3).getSampleSpacing();
        
        double maxVals[] = new double[timepoints];
        double avgVals[] = new double[timepoints];
        double times[] = new double[timepoints];
        double overallMax = 0.0;
        int maxTimePoint = 0;
        for (int i=0; i<timepoints; i++) {
            maxVals[i] = data[thermometry.getVoxelOffset(cols/2, rows/2, i)];
            
            double maxVal = 0;
            double valueSum = 0;
            int counter = 0;
            for (int x=-1; x<=2; x++) {
                for (int y=-1; y<=2; y++) {
                    counter++;
                    double val = data[thermometry.getVoxelOffset(cols/2+x, rows/2+y, i)];
                    valueSum += val;
                    if (x==0 || x==1 || y==0 || y==1) {
                        counter++;
                        valueSum += val;        
                    }
                    if (val > maxVal) {
                        maxVal = val;
                    }      
                }
            }
            
            maxVals[i] = maxVal;
            avgVals[i] = valueSum/(float)counter;
            times[i] = i*phaseTime;
            
            if (maxVal > overallMax) {
                overallMax = maxVal;
                maxTimePoint = i;
            }
            
            System.out.println("Thermometry " + times[i] + " s, " + maxVals[i] + ", " + avgVals[i]);
        }
        
        model.getSonication(sonicationIndex).setAttribute("maxFrame", maxTimePoint);
        if (setToMax) {
            model.getSonication(sonicationIndex).setAttribute("currentFrame", maxTimePoint);
        }

        thermometryChart.newChart();
        thermometryChart.addSeries("Max", times, maxVals, new Vector4f(0.8f, 0.2f, 0.2f, 1f));
        thermometryChart.addSeries("Avg", times, avgVals, new Vector4f(0.2f, 0.8f, 0.2f, 1f));
        thermometryChart.generateChart();
        
        showThermometry(sonicationIndex);
    }
    
    private void updateMRlist() {
        System.out.println("updateMRList");
        
        this.mrSeriesSelector.clear();
        mrSeriesSelector.setTitle("MR Series");
        for (int i=0; i<model.getMrImageCount(); i++) {
            try {
                mrSeriesSelector.addItem(i, model.getMrImage(i).getAttribute("ProtocolName").toString());
            }
            catch(Exception e) {
                mrSeriesSelector.addItem(i, "Unspecified MR protocol");                
            }
        }
//        if (mrSeriesSelector.getSelectionIndex() != 0) {
            mrSeriesSelector.setSelectionIndex(0);
//        }
    }
    
    private void updateSonicationList() {
        System.out.println("updateSonicationList");
        
        this.sonicationSelector.clear();
        sonicationSelector.setTitle("Sonication 1");
        for (int i=0; i<model.getSonicationCount(); i++) {
            try {
                sonicationSelector.addItem(i, "Sonication " + (i + 1) + " (" + Math.round(model.getSonication(i).getPower() * 10f) / 10f + "W)");
            }
            catch(Exception e) {
            }
        }
        sonicationSelector.setSelectionIndex(0);
    }

    @Override
    public void processInput() {
        while (Mouse.next()) {
            OnMouse(Mouse.getEventX(), Mouse.getEventY(), Mouse.isButtonDown(0), Mouse.isButtonDown(1), Mouse.getEventDWheel());
        }
        
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x-0.1f, steering.y, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
           // model.setAttribute("currentTargetSteering", steering.translate(-0.1f, 0f, 0f));
            
           // updateTargetAndSteering();
           // updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_center -= 2;
//                model.setAttribute("MRcenter", (float)mr_center);
//                System.out.println("MR Center = " + mr_center);
//            } else {
//                center -= 10;
//                model.setAttribute("CTcenter", (float)center);
////                System.out.println("Center = " + center);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x+0.1f, steering.y, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0.1f, 0f, 0f));
            
//            System.out.println("************** Steering: " + steering);
            
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_center += 2;
//                model.setAttribute("MRcenter", (float)mr_center);
//                System.out.println("MR Center = " + mr_center);
//            } else {
//                center += 10;
//                model.setAttribute("CTcenter", (float)center);
////                System.out.println("Center = " + center);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x, steering.y+0.1f, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0f, 0.1f, 0f));
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_window += 10;
//                model.setAttribute("MRwindow", (float)mr_window);
////                System.out.println("MR Window = " + mr_window);
//            } else {
//                window += 10;
//                model.setAttribute("CTwindow", (float)window);
//                System.out.println("Window = " + window);
//            }
// //           needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x, steering.y-0.1f, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0f, -0.1f, 0f));
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_window -= 10;
//                model.setAttribute("MRwindow", (float)mr_window);
//                System.out.println("MR Window = " + mr_window);
//            } else {
//                window -= 10;
//                model.setAttribute("CTwindow", (float)window);
////                System.out.println("Window = " + window);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_7)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                mr_threshold += 5;
                model.setAttribute("MRthresh", (float)mr_threshold);
            } else {
                ct_threshold += 5;
                model.setAttribute("CTthresh", (float)mr_threshold);
            }
//            needsRendering = true;
            System.out.println(" CT Thresh = " + ct_threshold);
            System.out.println(" MR Thresh = " + mr_threshold);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_8)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                mr_threshold -= 5;
                model.setAttribute("MRthresh", (float)mr_threshold);
            } else {
                ct_threshold -= 5;
                model.setAttribute("CTthresh", (float)mr_threshold);
            }
//            needsRendering = true;
            System.out.println("Thresh = " + ct_threshold);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_9)) {
//            needsRendering = true;
            canvas.setForegroundVolumeSlices(Math.min(600, canvas.getForegroundVolumeSlices() + 10));
            model.setAttribute("foregroundVolumeSlices", (float)canvas.getForegroundVolumeSlices());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
//            needsRendering = true;
            canvas.setForegroundVolumeSlices(Math.max(0, canvas.getForegroundVolumeSlices() - 10));
            model.setAttribute("foregroundVolumeSlices", (float)canvas.getForegroundVolumeSlices());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_3)) {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                transRayTracer.setBoneSpeed((float)Math.round(transRayTracer.getBoneSpeed()/5f)*5f - 5f);
                model.setAttribute("boneSOS", transRayTracer.getBoneSpeed());
    //            needsRendering = true;
                System.out.println("Bone speed: " + transRayTracer.getBoneSpeed());
            }
            else {
                transRayTracer.setBoneRefractionSpeed((float)Math.round(transRayTracer.getBoneRefractionSpeed()/5f)*5f - 5f);
                model.setAttribute("boneRefractionSOS", transRayTracer.getBoneRefractionSpeed());
    //            needsRendering = true;
                System.out.println("Bone refraction speed: " + transRayTracer.getBoneRefractionSpeed());
                
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_4)) {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                transRayTracer.setBoneSpeed((float)Math.round(transRayTracer.getBoneSpeed()/5f)*5f + 5f);
                model.setAttribute("boneSOS", transRayTracer.getBoneSpeed());
    //            needsRendering = true;
                System.out.println("Bone speed: " + transRayTracer.getBoneSpeed());
            }
            else {
                transRayTracer.setBoneRefractionSpeed((float)Math.round(transRayTracer.getBoneRefractionSpeed()/5f)*5f + 5f);
                model.setAttribute("boneRefractionSOS", transRayTracer.getBoneRefractionSpeed());
    //            needsRendering = true;
                System.out.println("Bone refraction speed: " + transRayTracer.getBoneRefractionSpeed());
                
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_T)) {
//            needsRendering = true;
            transducerTilt += 0.5f;
            transducerModel.setTransducerXTilt(transducerTilt);
            model.setAttribute("transducerXTilt", transducerTilt);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_G)) {
//            needsRendering = true;
            transducerTilt -= 0.5f;
            transducerModel.setTransducerXTilt(transducerTilt);
            model.setAttribute("transducerXTilt", transducerTilt);
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_D) {
                    transducerModel.setShowSteeringVolume(!this.transducerModel.getShowSteeringVolume());
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_F) {
                    transducerModel.setShowFocalVolume(!this.transducerModel.getShowFocalVolume());
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_S) {
                    //System.out.println("1 Key Pressed");
                    saveScene();
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_A) {
                    //System.out.println("1 Key Pressed");
                    loadScene();
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_Z) {
                    //System.out.println("1 Key Pressed");
                    zoomAnimator.set(dolly, dolly.getValue(), -35f, 4f);
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_1) {
                    //System.out.println("1 Key Pressed");
//                    needsRendering = true;
                    selTrans = ++selTrans % Transducer.getTransducerDefCount();

                    transRayTracer.release();
                    setDoTransition(true);;
//                    if (selTrans == 0) {
//                        transRayTracer.init(transducer220);
//                    } else {
//                        transRayTracer.init(transducer650);
//                    }
                    
//                    this.transducerModel.setTransducerTilt(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0));
                    transRayTracer.init(transducerModel.buildElements(Transducer.getTransducerDef(selTrans)));
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_H) {
                    //System.out.println("1 Key Pressed");
//                    needsRendering = true;
                    canvas.setShowMR(!canvas.getShowMR());
                    setDoTransition(true);;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_2) {
                    //System.out.println("2 Key Pressed");
//                    needsRendering = true;
                    drawingStyle = 1 - drawingStyle;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_N) {
                    //System.out.println("2 Key Pressed");
//                    needsRendering = true;
                    canvas.setVolumeRender(!canvas.getVolumeRender());
                    ctHistogram.calculate();
                    transFuncDisplay.setHistogram(ctHistogram.getData());
                    ctHistogram.release();
                    setDoTransition(true);;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_5) {
                    //System.out.println("5 Key Pressed");
//                    needsRendering = true;
                    transducerModel.setShowRays(!transducerModel.getShowRays());
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_6) {
                    //System.out.println("5 Key Pressed");
//                    needsRendering = true;
                    
                    try {
                        showRayTracer = (Boolean)model.getAttribute("showRayTracer");
                    }
                    catch(NullPointerException e) {
                        showRayTracer = false;
                    }
                    showRayTracer = !showRayTracer;
                    model.setAttribute("showRayTracer", showRayTracer);
//                    if (!showRayTracer) {
//                        activeElementsBar.setValue(-1f);
//                        sdrBar.setValue(-1);
//                    }
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_C) {
                    //System.out.println("C Key Pressed");
//                    needsRendering = true;
                    try {
                        doClip = (Boolean)model.getAttribute("doClip");
                    }
                    catch(NullPointerException e) {
                        doClip = false;
                    }
                    doClip = !doClip;
                    model.setAttribute("doClip", doClip);
                    
                    
                    transducerModel.setClipRays(doClip);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_X) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    try {
                        doFrame = (Boolean)model.getAttribute("doFrame");
                    }
                    catch(NullPointerException e) {
                        doFrame = false;
                    }
                    doFrame = !doFrame;
                    model.setAttribute("doFrame", doFrame);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_V) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    showScanner = !showScanner;
                    scene.setIsDirty(true);
                    setDoTransition(true);;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_M) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    try {
                        doMRI = (Boolean)model.getAttribute("doMRI");
                    }
                    catch(NullPointerException e) {
                        doMRI = false;
                    }
                    doMRI = !doMRI;
                    model.setAttribute("doMRI", doMRI);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_Q) {
                    initController();
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_P) {
//                    boolean doEnv = !transRayTracer.getShowEnvelope();
                    transRayTracer.setShowEnvelope(false);

//                    if (doEnv) {
                    Vector3f CofR = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
                    
                    canvas.setShowPressure(false);
                    canvas.setTextureRotatation(CofR.translate(currentSteering.getXpos(), currentSteering.getYpos(), currentSteering.getZpos()), trackball);

                    transRayTracer.setTextureRotatation(CofR, trackball);
                    transRayTracer.calcEnvelope(this.controller);

                    canvas.setOverlayImage(transRayTracer.getEnvelopeImage());
                    canvas1.setOverlayImage(transRayTracer.getEnvelopeImage());
                    canvas2.setOverlayImage(transRayTracer.getEnvelopeImage());
                    canvas3.setOverlayImage(transRayTracer.getEnvelopeImage());
//                    }
                    //System.out.println("P Key Pressed");
//                      MRCTRegister regObj = new MRCTRegister();
//                      Quaternion transform = regObj.register(this.ctImage, this.mrImage);
//                      
//                      this.ctImage.setAttribute("ImageOrientationQ", transform);
//                      
//                      Vector3f trans = regObj.getTranslation();
//                      
//                      this.ctImage.setAttribute("ImageTranslation", new Vector3f(trans));
//                      
//                    needsRendering = true;

//                    ctHistogram.calculate();                    
//                    transFuncDisplay.setHistogram(ctHistogram.getData());
//                    ctHistogram.release();
//                    needsRendering = true;
                    setDoTransition(true);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_L) {
                    boolean bDoPressure = canvas.getShowPressure();
                    if (bDoPressure == false) {
                        transRayTracer.setShowEnvelope(false); // TODO: this is probably overtaken by events now. remove?
                        canvas.setShowPressure(true);
                        canvas.setShowThermometry(false);
                        updateTargetAndSteering();
                        updatePressureCalc();
                        setDoTransition(true);;
                    }
                    else {
                        transRayTracer.setShowEnvelope(false);
                        canvas.setShowPressure(false);
                        canvas.setShowThermometry(false);
                        setDoTransition(true);;
                    }
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_K) {
                    controller.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "loadExport"));
                }

            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_I) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle(new String("Choose CT file..."));
                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    File ctfile = null;
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        ctfile = fileChooser.getSelectedFile();
                        System.out.println("Selected file: " + ctfile.getAbsolutePath());

                        ctloader = new Loader();
                        ctloader.load(ctfile, "CT_IMAGE_LOADED", controller);
                    }
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_O) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle(new String("Choose MR file..."));
                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    File mrfile = null;
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        mrfile = fileChooser.getSelectedFile();
                        System.out.println("Selected file: " + mrfile.getAbsolutePath());

                        mrloader = new Loader();
                        mrloader.load(mrfile, "MR_IMAGE_0_LOADED", controller);
                    }
                }
            }
        }            
        
        processController();
    }
    
    public void pressureCalCalledByButton(){
        transRayTracer.setShowEnvelope(false); // TODO: this is probably overtaken by events now. remove?
        canvas.setShowPressure(true);
        // updatePressureCalc();
        
        if (canvas.getShowPressure()) {
            Vector3f CofR = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
            canvas.setTextureRotatation(CofR.translate(currentSteering.getXpos(), currentSteering.getYpos(), currentSteering.getZpos()), trackball);
            this.transRayTracer.setTextureRotatation(CofR, trackball);
//TODO: put back later
//            this.transRayTracer.attenuation_term_on = attenuation_term_on;
//            this.transRayTracer.transmissionLoss_term_on = transmissionLoss_term_on;
//            
//            this.transRayTracer.calcPressureEnvelope3D();
            //  this.transRayTracer.calcPressureEnvelope();
            canvas.setOverlayImage(transRayTracer.getEnvelopeImage());

            canvas1.setOverlayImage(transRayTracer.getEnvelopeImage());
            canvas2.setOverlayImage(transRayTracer.getEnvelopeImage());
            canvas3.setOverlayImage(transRayTracer.getEnvelopeImage());
        }
        
        System.out.println("pressure button pressed");
    }
 
    private float processAxisInput(net.java.games.input.Controller c, net.java.games.input.Component.Identifier id) {
        float data = c.getComponent(id).getPollData();
        if (Math.abs(data) < 0.1d) return 0f;
        return data;
    }
    
    private float processTriggerInput(net.java.games.input.Controller c, net.java.games.input.Component.Identifier id) {
        float data;
        try {
            data = (c.getComponent(id).getPollData() + 1f)/2f;
        }
        catch(NullPointerException e) {
//            e.printStackTrace();
            data = 0;
        }
        if (data < 0.1d || Math.abs(data-0.5d) < 1E-4) return 0f;
        return data;
    }
    
    public void saveSkullParams() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(new String("Choose file for skull params..."));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setSelectedFile(new File("skullParams.txt"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File outFile = null;
        if (fileChooser.showSaveDialog(Display.getParent()) == JFileChooser.APPROVE_OPTION) {
            outFile = fileChooser.getSelectedFile();
            this.transRayTracer.writeSkullMeasuresFile(outFile);


            try {
                java.awt.Desktop desktop = Desktop.getDesktop();
                if (desktop != null) {                
                    try {
                        desktop.open(new File(outFile.getAbsolutePath()));
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
        }
    }
    public void saveACTFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(new String("Save ACT..."));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("ACT.ini"));
        File outFile = null;
        if (fileChooser.showSaveDialog(Display.getParent()) == JFileChooser.APPROVE_OPTION) {
            outFile = fileChooser.getSelectedFile();
            
            Vector3f steering = transRayTracer.getTargetSteering();
            
            Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
            Vector3f spotPosition = Vector3f.add(naturalFocusPosition, steering, null);
            
            canvas.setTextureRotatation(spotPosition, trackball);
            
            this.transRayTracer.setTextureRotatation(naturalFocusPosition, trackball);
            
            this.transRayTracer.calcPressureEnvelope();
            this.transRayTracer.writeACTFile(outFile);


            try {
                java.awt.Desktop desktop = Desktop.getDesktop();
                if (desktop != null) {                
                    try {
                        desktop.open(new File(outFile.getAbsolutePath()));
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
        }
    }
    
    private void processController() {
        if (gameController != null) {
            try {
                if (!gameController.poll()) {
                    gameController = null;
                    return;
                }
            }
            catch(Exception e) {
                gameController = null;
                return;
            }
            
            float xrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.Y);
            float yrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.X);
            float zrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.RX);
//            float zoom =  processAxisInput(controller, net.java.games.input.Component.Identifier.Axis.RY);
            float zoom =  processTriggerInput(gameController, net.java.games.input.Component.Identifier.Axis.Z);
            if (zoom == 0f) {
                zoom =  -processTriggerInput(gameController, net.java.games.input.Component.Identifier.Axis.RZ);   
            }
                        
            if (xrot != 0f || yrot != 0f || zrot != 0f || zoom != 0f) {
                //System.out.println(xrot);
                
                Quaternion q = this.trackball.getCurrent();
                Matrix4f mat = Trackball.toMatrix4f(q);
                Matrix4f.transpose(mat, mat);
                
                //mat = Matrix4f.setIdentity(mat);
                
                mat = Matrix4f.rotate(-xrot/20f, new Vector3f(1f, 0f, 0f), mat, null);
                mat = Matrix4f.rotate(yrot/20f, new Vector3f(0f, 1f, 0f), mat, null);
                mat = Matrix4f.rotate(-zrot/20f, new Vector3f(0f, 0f, 1f), mat, null);
                Quaternion.setFromMatrix(mat, q);
                q = q.normalise(null);
                
                trackball.setCurrent(q);
                
                dolly.incrementValue(zoom*10f);
                
                mainLayer.setIsDirty(true);

//                needsRendering = true;
            }
            
            float pov = gameController.getComponent(net.java.games.input.Component.Identifier.Axis.POV).getPollData();
            if (pov == net.java.games.input.Component.POV.DOWN) {
                canvas.setForegroundVolumeSlices(Math.min(600, canvas.getForegroundVolumeSlices()+10));
                if (canvas.getForegroundVolumeSlices()<=50) {
                    transRayTracer.setClipRays(true);
                }
                else {
                   transRayTracer.setClipRays(false);
                }
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.UP) {
                canvas.setForegroundVolumeSlices(Math.max(0, canvas.getForegroundVolumeSlices()-10));
                if (canvas.getForegroundVolumeSlices()<=50) {
                    transRayTracer.setClipRays(true);
                }
                else {
                   transRayTracer.setClipRays(false);
                }
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.LEFT) {
                int ot = transFuncDisplay.getOpacityThreshold() - 1;
                transFuncDisplay.setOpacityThreshold(ot);
                transFuncDisplay.setMaterialThreshold(ot-20);
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.RIGHT) {
                int ot = transFuncDisplay.getOpacityThreshold() + 1;
                transFuncDisplay.setOpacityThreshold(ot);
                transFuncDisplay.setMaterialThreshold(ot-20);
//                needsRendering = true;                
//                needsRendering = true;
            }
            
            net.java.games.input.EventQueue queue = gameController.getEventQueue();
            net.java.games.input.Event event = new net.java.games.input.Event();
            while (queue.getNextEvent(event)) {
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._0) {
                    if (event.getValue() == 1f) {
                        try {
                            doClip = (Boolean)model.getAttribute("doClip");
                        }
                        catch(NullPointerException e) {
                            doClip = false;
                        }
                        doClip = !doClip;
                        model.setAttribute("doClip", doClip);


                        transducerModel.setClipRays(doClip);
                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._1) {
                    if (event.getValue() == 1f) {
                        try {
                            showRayTracer = (Boolean)model.getAttribute("showRayTracer");
                        }
                        catch(NullPointerException e) {
                            showRayTracer = false;
                        }
                        showRayTracer = !showRayTracer;
                        model.setAttribute("showRayTracer", showRayTracer);
                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._2) {
                    if (event.getValue() == 1f) {
                        try {
                            doFrame = (Boolean)model.getAttribute("doFrame");
                        }
                        catch(NullPointerException e) {
                            doFrame = false;
                        }
                        doFrame = !doFrame;
                        model.setAttribute("doFrame", doFrame);

                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._3) {
                    if (event.getValue() == 1f) {
//                        needsRendering = true;
                        canvas.setShowMR(!canvas.getShowMR());
                    }
                }
            }
        }
    }
    
    private static net.java.games.input.ControllerEnvironment createDefaultEnvironment() throws ReflectiveOperationException {

        // Find constructor (class is package private, so we can't access it directly)
        Constructor<net.java.games.input.ControllerEnvironment> constructor = (Constructor<net.java.games.input.ControllerEnvironment>)
            Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];

        // Constructor is package private, so we have to deactivate access control checks
        constructor.setAccessible(true);

        // Create object with default constructor
        return constructor.newInstance();
    }
    
    // Init game controller UI if present
    private static void initController() {
        net.java.games.input.Controller[] controls;
        try {
            
                try {
                    controllerEnvironement = createDefaultEnvironment();
                    controls = controllerEnvironement.getControllers();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    return;
                }
//            }

            
            for (int i=0; i<controls.length; i++) {
                System.out.println(controls[i].getName());
                if (controls[i].getName().startsWith("Controller")) {
                    gameController = controls[i];
                    break;
                }
                gameController = null;
            }
            
            if (gameController != null) {
                net.java.games.input.Component comps[] = gameController.getComponents();
                for (int i=0; i<comps.length; i++) {
                    System.out.println(comps[i].getName() + " - " + comps[i].getIdentifier());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            gameController = null;
        }
    }        

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        switch(command) {
            case "saveSkullParams":
                saveSkullParams();
                break;
            case "saveACTfile":
                saveACTFile();
                break;
            case "currentOverlayFrame":
                Integer sonicationIndex = (Integer)model.getAttribute("currentSonication");
                if (sonicationIndex == null) {
                    sonicationIndex = 0;
                }

                int nFrames = 1;
                try {
                    nFrames = model.getSonication(sonicationIndex).getThermometryPhase().getDimension(3).getSize();
                    model.getSonication(sonicationIndex).setAttribute("currentFrame", (int)(this.thermometryChart.getSelectedXValue()*(nFrames-1)));
                    updateThermometryDisplay(sonicationIndex, false);
                }
                catch(Exception ex) {
                }
                break;
            case "rotateScene":
                this.currentMouseMode = mouseMode.SCENE_ROTATE;
                break;
            case "rotateHead":
                this.currentMouseMode = mouseMode.HEAD_ROTATE;
                break;
            case "rotateSkull":
                this.currentMouseMode = mouseMode.SKULL_ROTATE;
                break;
            case "rotateFrame":
                this.currentMouseMode = mouseMode.FRAME_ROTATE;
                break;
            case "translateHead":
                this.currentMouseMode = mouseMode.HEAD_TRANSLATE;
                break;
            case "translateSkull":
                this.currentMouseMode = mouseMode.SKULL_TRANSLATE;
                break;
            case "translateFrame":
                this.currentMouseMode = mouseMode.FRAME_TRANSLATE;
                break;
        }
    }
}