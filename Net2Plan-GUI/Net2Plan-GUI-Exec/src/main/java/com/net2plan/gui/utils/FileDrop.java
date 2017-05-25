/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.io.*;
import java.net.URI;
import java.util.EventObject;
import java.util.List;

/**
 * <p>This class makes it easy to drag and drop files from the operating system to
 * a Java program. Any <tt>Component</tt> can be dropped onto, but only
 * <tt>javax.swing.JComponent</tt>s will indicate the drop event with a changed
 * border.</p>
 * <p>To use this class, construct a new <tt>FileDrop</tt> by passing it the target
 * component and a <tt>Listener</tt> to receive notification when file(s) have
 * been dropped. Here is an example:</p>
 * <p>
 * {@code <pre>
 *      JPanel myPanel = new JPanel();
 *      new FileDrop( myPanel, new FileDrop.Listener()
 *      {   public void filesDropped( java.io.File[] files )
 *          {
 *              // handle file drop
 *              ...
 *          }   // end filesDropped
 *      }); // end FileDrop.Listener
 * </pre>}
 * </p>
 * <p>You can specify the border that will appear when files are being dragged by
 * calling the constructor with a <tt>Border</tt>. Only
 * <tt>JComponent</tt>s will show any indication with a border.</p>
 * <p>You can turn on some debugging features by passing a <tt>PrintStream</tt>
 * object (such as <tt>System.out</tt>) into the full constructor. A
 * <tt>null</tt> value will result in no extra debugging information being output.</p>
 * <p>I'm releasing this code into the Public Domain. Enjoy.</p>
 * <p><em>Original author: Robert Harder, rharder@usa.net</em></p>
 * <p>2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.</p>
 * <p>2015-02-15 Jose-Luis Izquierdo-Zaragoza -- setActive() method added to
 * enable/disable DnD.</p>
 *
 * @author Robert Harder
 * @author rharder@users.sf.net
 * @version 1.0.1
 */
@SuppressWarnings("unchecked")
public class FileDrop {
    private transient Border normalBorder;
    private transient java.awt.dnd.DropTargetListener dropListener;
    private PrintStream out;
    private Component c;
    private boolean recursive;

    /**
     * Discover if the running JVM is modern enough to have drag and drop.
     */
    private static Boolean supportsDnD;

    // Default border color
    private static java.awt.Color defaultBorderColor = new java.awt.Color(0f, 0f, 1f, 0.25f);

    /**
     * Constructs a {@link FileDrop} with a default light-blue border and, if
     * <var>c</var> is a {@link java.awt.Container}, recursively sets all
     * elements contained within as drop targets, though only the top level
     * container will change borders.
     *
     * @param c        Component on which files will be dropped.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(Component c, Listener listener) {
        this(null, c, javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), true, listener);
    }

    /**
     * Constructor with a default border and the option to recursively set drop
     * targets. If your component is a <tt>java.awt.Container</tt>, then each of
     * its children components will also listen for drops, though only the
     * parent will change borders.
     *
     * @param c         Component on which files will be dropped.
     * @param recursive Recursively set children as drop targets.
     * @param listener  Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(Component c, boolean recursive, Listener listener) {
        this(null, c, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), recursive, listener);
    }

    /**
     * Constructor with a default border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for the
     * parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out      PrintStream to record debugging info or null for no debugging.
     * @param c        Component on which files will be dropped.
     * @param listener Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(PrintStream out, Component c, Listener listener) {
        this(out, c, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), false, listener);
    }

    /**
     * Constructor with a default border, debugging optionally turned on and the
     * option to recursively set drop targets. If your component is a
     * <tt>java.awt.Container</tt>, then each of its children components will
     * also listen for drops, though only the parent will change borders. With
     * Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for the
     * parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out       PrintStream to record debugging info or null for no debugging.
     * @param c         Component on which files will be dropped.
     * @param recursive Recursively set children as drop targets.
     * @param listener  Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(PrintStream out, Component c, boolean recursive, Listener listener) {
        this(out, c, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), recursive, listener);
    }

    /**
     * Constructor with a specified border
     *
     * @param c          Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging
     *                   occurs.
     * @param listener   Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(Component c, Border dragBorder, Listener listener) {
        this(null, c, dragBorder, false, listener);
    }

    /**
     * Constructor with a specified border and the option to recursively set
     * drop targets. If your component is a <tt>java.awt.Container</tt>, then
     * each of its children components will also listen for drops, though only
     * the parent will change borders.
     *
     * @param c          Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging
     *                   occurs.
     * @param recursive  Recursively set children as drop targets.
     * @param listener   Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(Component c, Border dragBorder, boolean recursive, Listener listener) {
        this(null, c, dragBorder, recursive, listener);
    }

    /**
     * Constructor with a specified border and debugging optionally turned on.
     * With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for the
     * parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out        PrintStream to record debugging info or null for no debugging.
     * @param c          Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param listener   Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(PrintStream out, Component c, Border dragBorder, Listener listener) {
        this(out, c, dragBorder, false, listener);
    }

    /**
     * Full constructor with a specified border and debugging optionally turned
     * on. With Debugging turned on, more status messages will be displayed to
     * <tt>out</tt>. A common way to use this constructor is with
     * <tt>System.out</tt> or <tt>System.err</tt>. A <tt>null</tt> value for the
     * parameter <tt>out</tt> will result in no debugging output.
     *
     * @param out        PrintStream to record debugging info or null for no debugging.
     * @param c          Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param recursive  Recursively set children as drop targets.
     * @param listener   Listens for <tt>filesDropped</tt>.
     * @since 1.0
     */
    public FileDrop(final PrintStream out, final Component c, final Border dragBorder, final boolean recursive, final Listener listener) {
        if (supportsDnD()) {
            this.out = out;
            this.c = c;
            this.recursive = recursive;

            // Make a drop listener
            dropListener = new java.awt.dnd.DropTargetListener() {
                Cursor cursor;

                @Override
                public void dragEnter(java.awt.dnd.DropTargetDragEvent evt) {
                    log(out, "FileDrop: dragEnter event.");

                    // Is this an acceptable drag event?
                    if (isDragOk(out, evt)) {
                        // If it's a Swing component, set its border
                        if (c instanceof javax.swing.JComponent) {
                            javax.swing.JComponent jc = (javax.swing.JComponent) c;
                            normalBorder = jc.getBorder();
                            log(out, "FileDrop: normal border saved.");
                            jc.setBorder(dragBorder);
                            log(out, "FileDrop: drag border set.");
                        }   // end if: JComponent

                        // Acknowledge that it's okay to enter
                        //evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                        evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                        log(out, "FileDrop: event accepted.");
                    } // end if: drag ok
                    else {   // Reject the drag event
                        evt.rejectDrag();
                        log(out, "FileDrop: event rejected.");
                    }   // end else: drag not ok
                }   // end dragEnter

                @Override
                public void dragOver(java.awt.dnd.DropTargetDragEvent evt) {
                }

                @Override
                public void drop(java.awt.dnd.DropTargetDropEvent evt) {
                    log(out, "FileDrop: drop event.");
                    try {   // Get whatever was dropped
                        Transferable tr = evt.getTransferable();

                        // Is it a file list?
                        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // Say we'll take it.
                            //evt.acceptDrop ( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                            evt.acceptDrop(DnDConstants.ACTION_COPY);
                            log(out, "FileDrop: file list accepted.");

                            // Get a useful list
                            java.util.List fileList = (java.util.List) tr.getTransferData(DataFlavor.javaFileListFlavor);

                            // Convert list to array
                            java.io.File[] filesTemp = new java.io.File[fileList.size()];
                            fileList.toArray(filesTemp);
                            final java.io.File[] files = filesTemp;

                            // Alert listener to drop.
                            if (listener != null) listener.filesDropped(files);

                            // Mark that drop is completed.
                            evt.getDropTargetContext().dropComplete(true);
                            log(out, "FileDrop: drop complete.");
                        } // end if: file list
                        else // this section will check for a reader flavor.
                        {
                            // Thanks, Nathan!
                            // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                            DataFlavor[] flavors = tr.getTransferDataFlavors();
                            boolean handled = false;
                            for (DataFlavor flavor : flavors) {
                                if (flavor.isRepresentationClassReader()) {
                                    // Say we'll take it.
                                    //evt.acceptDrop ( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                                    evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                                    log(out, "FileDrop: reader accepted.");
                                    Reader reader = flavor.getReaderForText(tr);
                                    BufferedReader br = new BufferedReader(reader);
                                    if (listener != null) listener.filesDropped(createFileArray(br, out));

                                    // Mark that drop is completed.
                                    evt.getDropTargetContext().dropComplete(true);
                                    log(out, "FileDrop: drop complete.");
                                    handled = true;
                                    break;
                                }
                            }
                            if (!handled) {
                                log(out, "FileDrop: not a file list or reader - abort.");
                                evt.rejectDrop();
                            }
                            // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
                        }
                    } catch (java.io.IOException io) {
                        log(out, "FileDrop: IOException - abort:");
                        io.printStackTrace(out);
                        evt.rejectDrop();
                    } catch (UnsupportedFlavorException ufe) {
                        log(out, "FileDrop: UnsupportedFlavorException - abort:");
                        ufe.printStackTrace(out);
                        evt.rejectDrop();
                    } finally {
                        // If it's a Swing component, reset its border
                        if (c instanceof javax.swing.JComponent) {
                            javax.swing.JComponent jc = (javax.swing.JComponent) c;
                            jc.setBorder(normalBorder);
                            log(out, "FileDrop: normal border restored.");
                        }
                    }
                }

                @Override
                public void dragExit(java.awt.dnd.DropTargetEvent evt) {
                    log(out, "FileDrop: dragExit event.");

					/* If it's a Swing component, reset its border */
                    if (c instanceof javax.swing.JComponent) {
                        javax.swing.JComponent jc = (javax.swing.JComponent) c;
                        jc.setBorder(normalBorder);
                        log(out, "FileDrop: normal border restored.");
                    }
                }

                @Override
                public void dropActionChanged(java.awt.dnd.DropTargetDragEvent evt) {
                    log(out, "FileDrop: dropActionChanged event.");
                    // Is this an acceptable drag event?
                    if (isDragOk(out, evt)) {   //evt.acceptDrag( java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE );
                        evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                        log(out, "FileDrop: event accepted.");
                    } // end if: drag ok
                    else {
                        evt.rejectDrag();
                        log(out, "FileDrop: event rejected.");
                    }   // end else: drag not ok
                }   // end dropActionChanged
            }; // end DropTargetListener

            // Make the component (and possibly children) drop targets
            makeDropTarget(out, c, recursive);
        } // end if: supports dnd
        else {
            log(out, "FileDrop: Drag and drop is not supported with this JVM");
        }   // end else: does not support DnD
    }   // end constructor

    private static boolean supportsDnD() {   // Static Boolean
        if (supportsDnD == null) {
            boolean support;
            try {
                Class.forName("java.awt.dnd.DnDConstants");
                support = true;
            } // end try
            catch (Exception e) {
                support = false;
            }
            supportsDnD = support;
        }

        return supportsDnD;
    }

    // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
    private static String ZERO_CHAR_STRING = "" + (char) 0;

    private static File[] createFileArray(BufferedReader bReader, PrintStream out) {
        try {
            List list = new java.util.ArrayList();
            String line;
            while ((line = bReader.readLine()) != null) {
                try {
                    // kde seems to append a 0 char to the end of the reader
                    if (ZERO_CHAR_STRING.equals(line)) continue;

                    File file = new File(new URI(line));
                    list.add(file);
                } catch (Exception ex) {
                    log(out, "Error with " + line + ": " + ex.getMessage());
                }
            }

            return (java.io.File[]) list.toArray(new File[list.size()]);
        } catch (IOException ex) {
            log(out, "FileDrop: IOException");
        }

        return new File[0];
    }
    // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.

    private void makeDropTarget(final PrintStream out, final Component c, boolean recursive) {
        // Make drop target
        final DropTarget dt = new DropTarget();
        try {
            dt.addDropTargetListener(dropListener);
        } catch (java.util.TooManyListenersException e) {
            log(out, "FileDrop: Drop will not work due to previous error. Do you have another listener attached?");
        }

        // Listen for hierarchy changes and remove the drop target when the parent gets cleared out.
        c.addHierarchyListener(new java.awt.event.HierarchyListener() {
            @Override
            public void hierarchyChanged(java.awt.event.HierarchyEvent evt) {
                log(out, "FileDrop: Hierarchy changed.");
                Component parent = c.getParent();
                if (parent == null) {
                    c.setDropTarget(null);
                    log(out, "FileDrop: Drop target cleared from component.");
                } else {
                    new java.awt.dnd.DropTarget(c, dropListener);
                    log(out, "FileDrop: Drop target added to component.");
                }
            }
        });

        if (c.getParent() != null) new java.awt.dnd.DropTarget(c, dropListener);

        if (recursive && (c instanceof java.awt.Container)) {
            // Get the container
            java.awt.Container cont = (java.awt.Container) c;

            // Get it's components
            Component[] comps = cont.getComponents();

            // Set it's components as listeners also
            for (Component comp : comps) makeDropTarget(out, comp, recursive);
        }
    }

    /**
     * Sets the {@code FileDrop} as enabled or disabled.
     *
     * @param active {@code true} if the {@code FileDrop} should become active, and {@code false} otherwise
     * @since 1.1
     */
    public void setActive(boolean active) {
        if (supportsDnD()) makeDropTargetActive(out, c, recursive, active);
    }

    private void makeDropTargetActive(final PrintStream out, final Component c, boolean recursive, boolean active) {
        DropTarget dropTarget = c.getDropTarget();
        if (dropTarget != null) dropTarget.setActive(active);

        if (c.getParent() != null) {
            DropTarget parentDropTarget = c.getDropTarget();
            if (parentDropTarget != null) c.getDropTarget().setActive(active);
        }

        if (recursive && c instanceof Container) {
            // Get the container
            Container cont = (Container) c;

            // Get its components
            Component[] comps = cont.getComponents();

            // Set its components as listeners also
            for (Component comp : comps) makeDropTargetActive(out, comp, recursive, active);
        }
    }

    /**
     * Determine if the dragged data is a file list.
     */
    private boolean isDragOk(final PrintStream out, final DropTargetDragEvent evt) {
        boolean ok = false;

        // Get data flavors being dragged
        DataFlavor[] flavors = evt.getCurrentDataFlavors();

        // See if any of the flavors are a file list
        int i = 0;
        while (!ok && i < flavors.length) {
            // BEGIN 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
            // Is the flavor a file list?
            final DataFlavor curFlavor = flavors[i];
            if (curFlavor.equals(DataFlavor.javaFileListFlavor) || curFlavor.isRepresentationClassReader()) ok = true;

            // END 2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.
            i++;
        }   // end while: through flavors

        // If logging is enabled, show data flavors
        if (out != null) {
            if (flavors.length == 0) log(out, "FileDrop: no data flavors.");

            for (i = 0; i < flavors.length; i++)
                log(out, flavors[i].toString());
        }

        return ok;
    }

    /**
     * Outputs <tt>message</tt> to <tt>out</tt> if it's not null.
     */
    private static void log(PrintStream out, String message) {
        // Log message if requested
        if (out != null) out.println(message);
    }

    /**
     * Removes the drag-and-drop hooks from the component and optionally from
     * the all children. You should call this if you add and remove components
     * after you've set up the drag-and-drop. This will recursively unregister
     * all components contained within
     * <var>c</var> if <var>c</var> is a {@link java.awt.Container}.
     *
     * @param c The component to unregister as a drop target
     * @return {@code true} if hooks were removed, and {@code false} otherwise
     * @since 1.0
     */
    public static boolean remove(Component c) {
        return remove(null, c, true);
    }

    /**
     * Removes the drag-and-drop hooks from the component and optionally from
     * the all children. You should call this if you add and remove components
     * after you've set up the drag-and-drop.
     *
     * @param out       Optional {@link PrintStream} for logging drag and drop
     *                  messages
     * @param c         The component to unregister
     * @param recursive Recursively unregister components within a container
     * @return {@code true} if hooks were removed, and {@code false} otherwise
     * @since 1.0
     */
    public static boolean remove(PrintStream out, Component c, boolean recursive) {   // Make sure we support dnd.
        if (supportsDnD()) {
            log(out, "FileDrop: Removing drag-and-drop hooks.");
            c.setDropTarget(null);
            if (recursive && (c instanceof java.awt.Container)) {
                Component[] comps = ((java.awt.Container) c).getComponents();
                for (Component comp : comps) remove(out, comp, recursive);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

	/* ********  I N N E R   I N T E R F A C E   L I S T E N E R  ******** */

    /**
     * Implement this inner interface to listen for when files are dropped. For
     * example your class declaration may begin like this: 	 {@code <pre>
     *      public class MyClass implements FileDrop.Listener
     *      ...
     *      public void filesDropped( java.io.File[] files )
     *      {
     *          ...
     *      }   // end filesDropped
     *      ...
     * </pre>}
     *
     * @since 1.1
     */
    public static interface Listener {

        /**
         * This method is called when files have been successfully dropped.
         *
         * @param files An array of <tt>File</tt>s that were dropped.
         * @since 1.0
         */
        public void filesDropped(File[] files);

    }   // end inner-interface Listener

	/* ********  I N N E R   C L A S S  ******** */

    /**
     * This is the event that is passed to the
     * {@link FileDropListener#filesDropped filesDropped(...)} method in your
     * {@link FileDropListener} when files are dropped onto a registered drop
     * target.
     * <p>
     * I'm releasing this code into the Public Domain. Enjoy.</p>
     *
     * @author Robert Harder
     * @author rob@iharder.net
     * @version 1.2
     */
    public static class Event extends EventObject {
        private File[] files;

        /**
         * Constructs an {@link Event} with the array of files that were dropped
         * and the {@link FileDrop} that initiated the event.
         *
         * @param files  The array of files that were dropped
         * @param source The event source
         * @since 1.1
         */
        public Event(File[] files, Object source) {
            super(source);
            this.files = files;
        }

        /**
         * Returns an array of files that were dropped on a registered drop
         * target.
         *
         * @return array of files that were dropped
         * @since 1.1
         */
        public File[] getFiles() {
            return files;
        }

    }

	/* ********  I N N E R   C L A S S  ******** */

    /**
     * At last an easy way to encapsulate your custom objects for dragging and
     * dropping in your Java programs! When you need to create a
     * {@link java.awt.datatransfer.Transferable} object, use this class to wrap
     * your object. For example:
     * <pre>{@code
     *      ...
     *      MyCoolClass myObj = new MyCoolClass();
     *      Transferable xfer = new TransferableObject( myObj );
     *      ...
     * }</pre> Or if you need to know when the data was actually dropped,
     * like when you're moving data out of a list, say, you can use the
     * {@link TransferableObject.Fetcher} inner class to return your object Just
     * in Time. For example:
     * <pre>{@code
     *      ...
     *      final MyCoolClass myObj = new MyCoolClass();
     *
     *      TransferableObject.Fetcher fetcher = new TransferableObject.Fetcher()
     *      {   public Object getObject(){ return myObj; }
     *      }; // end fetcher
     *
     *      Transferable xfer = new TransferableObject( fetcher );
     *      ...
     * }</pre>
     * <p>
     * The {@link java.awt.datatransfer.DataFlavor} associated with
     * {@link TransferableObject} has the representation class
     * <tt>net.iharder.dnd.TransferableObject.class</tt> and MIME type
     * <tt>application/x-net.iharder.dnd.TransferableObject</tt>. This data
     * flavor is accessible via the static {@link #DATA_FLAVOR} property.
     * </p>
     * <p>
     * I'm releasing this code into the Public Domain. Enjoy.</p>
     *
     * @author Robert Harder
     * @author rob@iharder.net
     * @version 1.2
     */
    public static class TransferableObject implements Transferable {

        /**
         * The MIME type for {@link #DATA_FLAVOR} is
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @since 1.1
         */
        public final static String MIME_TYPE = "application/x-net.iharder.dnd.TransferableObject";

        /**
         * The default {@link java.awt.datatransfer.DataFlavor} for
         * {@link TransferableObject} has the representation class
         * <tt>net.iharder.dnd.TransferableObject.class</tt>
         * and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @since 1.1
         */
        public final static DataFlavor DATA_FLAVOR = new DataFlavor(TransferableObject.class, MIME_TYPE);

        private Fetcher fetcher;
        private Object data;

        private DataFlavor customFlavor;

        /**
         * Creates a new {@link TransferableObject} that wraps <var>data</var>.
         * Along with the {@link #DATA_FLAVOR} associated with this class, this
         * creates a custom data flavor with a representation class determined
         * from {@code data.getClass()} and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @param data The data to transfer
         * @since 1.1
         */
        public TransferableObject(Object data) {
            this.data = data;
            this.customFlavor = new DataFlavor(data.getClass(), MIME_TYPE);
        }   // end constructor

        /**
         * Creates a new {@link TransferableObject} that will return the object
         * that is returned by <var>fetcher</var>. No custom data flavor is set
         * other than the default {@link #DATA_FLAVOR}.
         *
         * @param fetcher The {@link Fetcher} that will return the data object
         * @see Fetcher
         * @since 1.1
         */
        public TransferableObject(Fetcher fetcher) {
            this.fetcher = fetcher;
        }

        /**
         * Creates a new {@link TransferableObject} that will return the object
         * that is returned by <var>fetcher</var>. Along with the
         * {@link #DATA_FLAVOR} associated with this class, this creates a
         * custom data flavor with a representation class <var>dataClass</var>
         * and the MIME type
         * <tt>application/x-net.iharder.dnd.TransferableObject</tt>.
         *
         * @param dataClass The {@link java.lang.Class} to use in the custom
         *                  data flavor
         * @param fetcher   The {@link Fetcher} that will return the data object
         * @see Fetcher
         * @since 1.1
         */
        public TransferableObject(Class dataClass, Fetcher fetcher) {
            this.fetcher = fetcher;
            this.customFlavor = new DataFlavor(dataClass, MIME_TYPE);
        }   // end constructor

        /**
         * Returns the custom {@link java.awt.datatransfer.DataFlavor}
         * associated with the encapsulated object or <tt>null</tt> if the
         * {@link Fetcher} constructor was used without passing a
         * {@link java.lang.Class}.
         *
         * @return The custom data flavor for the encapsulated object
         * @since 1.1
         */
        public DataFlavor getCustomDataFlavor() {
            return customFlavor;
        }

		/* ********  T R A N S F E R A B L E   M E T H O D S  ******** */

        /**
         * Returns a two- or three-element array containing first the custom
         * data flavor, if one was created in the constructors, second the
         * default {@link #DATA_FLAVOR} associated with
         * {@link TransferableObject}, and third the
         * {@link java.awt.datatransfer.DataFlavor.stringFlavor}.
         *
         * @return An array of supported data flavors
         * @since 1.1
         */
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (customFlavor != null) return new DataFlavor[]{customFlavor, DATA_FLAVOR, DataFlavor.stringFlavor};
            else return new DataFlavor[]{DATA_FLAVOR, DataFlavor.stringFlavor};
        }

        /**
         * Returns the data encapsulated in this {@link TransferableObject}. If
         * the {@link Fetcher} constructor was used, then this is when the
         * {@link Fetcher#getObject getObject()} method will be called. If the
         * requested data flavor is not supported, then the
         * {@link Fetcher#getObject getObject()} method will not be called.
         *
         * @param flavor The data flavor for the data to return
         * @return The dropped data
         * @throws java.awt.datatransfer.UnsupportedFlavorException Not accepted flavor exception
         * @since 1.1
         */
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            /* Native object */
            if (flavor.equals(DATA_FLAVOR)) return fetcher == null ? data : fetcher.getObject();

            // String
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return fetcher == null ? data.toString() : fetcher.getObject().toString();
            }

            // We can't do anything else
            throw new UnsupportedFlavorException(flavor);
        }

        /**
         * Returns <tt>true</tt> if <var>flavor</var> is one of the supported
         * flavors. Flavors are supported using the {@code equals(...)}
         * method.
         *
         * @param flavor The data flavor to check
         * @return Whether or not the flavor is supported
         * @since 1.1
         */
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            /* Native object */
            if (flavor.equals(DATA_FLAVOR)) return true;

            return flavor.equals(DataFlavor.stringFlavor);
        }


		/* ********  I N N E R   I N T E R F A C E   F E T C H E R  ******** */

        /**
         * Instead of passing your data directly to the
         * {@link TransferableObject} constructor, you may want to know exactly
         * when your data was received in case you need to remove it from its
         * source (or do anyting else to it). When the
         * {@link #getTransferData getTransferData(...)} method is called on the
         * {@link TransferableObject}, the {@link Fetcher}'s
         * {@link #getObject getObject()} method will be called.
         *
         * @author Robert Harder
         * @version 1.1
         * @copyright 2001
         * @since 1.1
         */
        public static interface Fetcher {
            /**
             * Return the object being encapsulated in the
             * {@link TransferableObject}.
             *
             * @return The dropped object
             * @since 1.1
             */
            public Object getObject();
        }
    }
}
