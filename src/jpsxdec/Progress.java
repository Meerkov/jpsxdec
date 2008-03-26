/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * Progress.java
 */

package jpsxdec;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;
import org.jdesktop.swingworker.SwingWorker;

public class Progress<T> extends javax.swing.JDialog implements PropertyChangeListener {

    /** The task to perform. */
    private SimpleWorker m_oTask;
    /** Holds the return value from the task. */
    private T m_oReturn;
    /** Holds any exception thrown by the task. */
    private Exception m_oException;
    
    private boolean m_blnTaskDone = false;
    
    /** Creates new form Progress */
    public Progress(java.awt.Dialog parent, String sDescription, SimpleWorker oTask) {
        super(parent, true);
        initComponents();
        guiDescriptionLbl.setText(sDescription);
        this.pack(); // repack after changing the label text
        this.setLocationRelativeTo(parent); // center on parent
        
        m_oTask = oTask;
        m_oTask.m_oParent = this;
        m_oTask.addPropertyChangeListener(this);
    }
    
    /** Creates new form Progress */
    public Progress(java.awt.Frame parent, String sDescription, SimpleWorker oTask) {
        super(parent, true);
        initComponents();
        guiDescriptionLbl.setText(sDescription);
        this.pack(); // repack after changing the label text
        this.setLocationRelativeTo(parent); // center on parent
        
        m_oTask = oTask;
        m_oTask.m_oParent = this;
        m_oTask.addPropertyChangeListener(this);
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        // update the progress bar
        if (evt.getPropertyName().equals("progress") ) {
            guiProgressBar.setValue((Integer)evt.getNewValue());
        } else if (evt.getPropertyName().equals("note") ) {
            guiProgressLbl.setText(evt.getNewValue().toString());
        } else if (evt.getPropertyName().equals("event") ) {
            guiEventLvl.setText(evt.getNewValue().toString());
        } else if (evt.getPropertyName().equals("return") ) {
            m_oReturn = (T)evt.getNewValue();
            TaskComplete();
        } else if (evt.getPropertyName().equals("error") ) {
            // non-fatal exception
            Exception ex = (Exception)evt.getNewValue();
            txtErrors.append(ex.toString() + "\n");
        } else if (evt.getPropertyName().equals("exception") ) {
            // fatal/unhandled exception
            m_oException = (Exception)evt.getNewValue();
            JOptionPane.showMessageDialog(this, m_oException.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
            //m_oException.printStackTrace(System.err); // debug
            TaskComplete();
        }        
    }
    
    private void TaskComplete() {
        m_blnTaskDone = true;
        if (txtErrors.getText().length() > 0) {
            guiCancelBtn.setText("Close");
        } else {
            this.setVisible(false);
            this.dispose();
        }
    }
    
    /** Returns the object returned from the task. */
    public T getResult() {
        return m_oReturn;
    }
    
    /** Returns if the cancel button was pressed before the task completed. */
    public boolean wasCanceled() {
        return m_oTask.isCancelled();
    }
    
    /** Returns if the task threw an exception. */
    public boolean threwException() {
        return m_oException != null;
    }
    
    /** Returns the exception thrown by the task (or null if none). */
    public Exception getException() {
        return m_oException;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        guiCancelBtn = new javax.swing.JButton();
        guiProgressBar = new javax.swing.JProgressBar();
        guiDescriptionLbl = new javax.swing.JLabel();
        guiProgressLbl = new javax.swing.JLabel();
        guiEventLvl = new javax.swing.JLabel();
        txtErrorsScroll = new javax.swing.JScrollPane();
        txtErrors = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Progress...");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setModal(true);
        setName("guiProgressDlg"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        guiCancelBtn.setText("Cancel");
        guiCancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiCancelBtnActionPerformed(evt);
            }
        });

        guiDescriptionLbl.setText("Description");

        guiProgressLbl.setText(" ");

        guiEventLvl.setText(" ");

        txtErrors.setColumns(20);
        txtErrors.setEditable(false);
        txtErrors.setRows(5);
        txtErrorsScroll.setViewportView(txtErrors);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(guiDescriptionLbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(guiEventLvl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(guiProgressLbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(guiProgressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(txtErrorsScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(guiCancelBtn))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(guiDescriptionLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiProgressLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiEventLvl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiProgressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiCancelBtn)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(txtErrorsScroll)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        m_oTask.execute();
    }//GEN-LAST:event_formWindowOpened

    private void guiCancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiCancelBtnActionPerformed
        if (m_blnTaskDone) {
            this.setVisible(false);
            this.dispose();
        } else {
            m_oTask.cancel(true);
            guiCancelBtn.setEnabled(false);
            // the task will trigger a 'done' or 'error' event once it's canceled
        }
}//GEN-LAST:event_guiCancelBtnActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton guiCancelBtn;
    private javax.swing.JLabel guiDescriptionLbl;
    private javax.swing.JLabel guiEventLvl;
    private javax.swing.JProgressBar guiProgressBar;
    private javax.swing.JLabel guiProgressLbl;
    private javax.swing.JTextArea txtErrors;
    private javax.swing.JScrollPane txtErrorsScroll;
    // End of variables declaration//GEN-END:variables


    /** Extention to simply the SwingWorker class even more. */
    public abstract static class SimpleWorker<T> extends SwingWorker<T, Void> {

        private Component m_oParent;
        
        /** To send updates to the progress form, 
         *  and check if cancel was pressed. */
        public static class TaskInfo {
            private java.awt.Component m_oProgressWindow;
            SimpleWorker m_oTask;
            public TaskInfo(SimpleWorker oTask, Component oParent)  
            { m_oTask = oTask; m_oProgressWindow = oParent; }
            // was cancel pressed?
            public boolean cancelPressed()       { return m_oTask.isCancelled(); }
            /** Send updates to the progress form. 
             * @param i  between 0 and 100. */
            public void updateProgress(int i) { m_oTask.setProgress(i); }
            private String m_sLastNote = null;
            public void updateNote(String s) {
                m_oTask.firePropertyChange("note", m_sLastNote, s);
                m_sLastNote = s;
            }
            private String m_sLastEvent = null;
            public void updateEvent(String s) {
                m_oTask.firePropertyChange("event", m_sLastEvent, s);
                m_sLastEvent = s;
            }
            public void showError(Exception e) {
                m_oTask.firePropertyChange("error", null, e);
            }
            
            public java.awt.Component getWindow() {
                return m_oProgressWindow;
            }
        }

        protected T doInBackground() throws Exception {
            try {
                T oRet = task(new TaskInfo(this, m_oParent));
                super.firePropertyChange("return", null, oRet);
                return oRet;
            } catch (Exception e) {
                super.firePropertyChange("exception", null, e);
                return null;
            }
        }

        abstract T task(final TaskInfo task) throws Exception;

    }
}
