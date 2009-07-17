/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.management.ui.views.queue;

import static org.apache.qpid.management.ui.Constants.CONSOLE_IMAGE;
import static org.apache.qpid.management.ui.Constants.RESULT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;

import org.apache.qpid.management.ui.ApiVersion;
import org.apache.qpid.management.ui.ApplicationRegistry;
import org.apache.qpid.management.ui.ManagedBean;
import org.apache.qpid.management.common.mbeans.ManagedQueue;
import org.apache.qpid.management.ui.jmx.JMXManagedObject;
import org.apache.qpid.management.ui.jmx.MBeanUtility;
import org.apache.qpid.management.ui.views.TabControl;
import org.apache.qpid.management.ui.views.ViewUtility;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


/**
 * Control class for the Queue mbean Operations tab.
 */
public class QueueOperationsTabControl extends TabControl
{
    private FormToolkit _toolkit;
    private ScrolledForm _form;
    private Table _table = null;
    private TableViewer _tableViewer = null;
    private Composite _paramsComposite = null;
            
    private ApiVersion _ApiVersion;
    
    private TabularDataSupport _messages = null;
    private ManagedQueue _qmb;
    
    static final String MSG_AMQ_ID = ManagedQueue.VIEW_MSGS_COMPOSITE_ITEM_NAMES[0];
    static final String MSG_HEADER = ManagedQueue.VIEW_MSGS_COMPOSITE_ITEM_NAMES[1];
    static final String MSG_SIZE = ManagedQueue.VIEW_MSGS_COMPOSITE_ITEM_NAMES[2];
    static final String MSG_REDELIVERED = ManagedQueue.VIEW_MSGS_COMPOSITE_ITEM_NAMES[3];
    static final String MSG_QUEUE_POS = ManagedQueue.VIEW_MSGS_COMPOSITE_ITEM_NAMES[4];
    
    public QueueOperationsTabControl(TabFolder tabFolder, JMXManagedObject mbean, MBeanServerConnection mbsc)
    {
        super(tabFolder);
        _mbean = mbean;
        _ApiVersion = ApplicationRegistry.getServerRegistry(mbean).getManagementApiVersion();
        _qmb = (ManagedQueue) MBeanServerInvocationHandler.newProxyInstance(mbsc, 
                                mbean.getObjectName(), ManagedQueue.class, false);
        _toolkit = new FormToolkit(_tabFolder.getDisplay());
        _form = _toolkit.createScrolledForm(_tabFolder);
        _form.getBody().setLayout(new GridLayout());
        createComposites();
        createWidgets();
    }
    
    private void createComposites()
    {
        _paramsComposite = _toolkit.createComposite(_form.getBody(), SWT.NONE);
        _paramsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        _paramsComposite.setLayout(new GridLayout());
    }
    
    /**
     * @see TabControl#getControl()
     */
    public Control getControl()
    {
        return _form;
    }
    
    /**
     * @see TabControl#setFocus()
     */
    public void setFocus()
    {
        _table.setFocus();
    }
    
    @Override
    public void refresh(ManagedBean mbean)
    {
        _messages = null;
        try
        {
            //gather a list of all messages on the queue for display and selection
            _messages = (TabularDataSupport) _qmb.viewMessages(1,Integer.MAX_VALUE);
            
            //TODO - viewMessages takes int args, limiting number of messages which can be viewed
            //to the first 2^32 messages on the queue at the time of invocation.
            //For consistency with other methods, expand values to Long by introducing a new method.
            //Use AMQ ID or current 'position in queue' numbering scheme ??
        }
        catch (Exception e)
        {
            MBeanUtility.handleException(mbean,e);
        }
        
        _tableViewer.setInput(_messages);

        layout();
    }
    
    public void layout()
    {
        _form.layout(true);
        _form.getBody().layout(true, true);
    }
    
    private void createWidgets()
    {
        Group messagesGroup = new Group(_paramsComposite, SWT.SHADOW_NONE | SWT.SCROLL_LINE);
        messagesGroup.setBackground(_paramsComposite.getBackground());
        messagesGroup.setText("Messages");
        messagesGroup.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        messagesGroup.setLayoutData(gridData);
        
        Composite tableAndButtonsComposite = _toolkit.createComposite(messagesGroup);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.minimumHeight = 220;
        gridData.heightHint = 220;
        tableAndButtonsComposite.setLayoutData(gridData);
        tableAndButtonsComposite.setLayout(new GridLayout(2,false));
               
        _table = new Table (tableAndButtonsComposite, SWT.MULTI | SWT.SCROLL_LINE | SWT.BORDER | SWT.FULL_SELECTION);
        _table.setLinesVisible (true);
        _table.setHeaderVisible (true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        _table.setLayoutData(data);
        
        _tableViewer = new TableViewer(_table);
        final TableSorter tableSorter = new TableSorter();
        
        String[] titles = {"AMQ ID", "Size(bytes)"};
        if(_ApiVersion.greaterThanOrEqualTo(1, 3))
        {
           //if server management API is >= 1.3, show message's queue position
           titles = new String[]{"AMQ ID", "Size(bytes)", "Queue Position"};
        }

        int[] bounds = { 175, 175, 140 };
        for (int i = 0; i < titles.length; i++) 
        {
            final int index = i;
            final TableColumn column = new TableColumn (_table, SWT.NONE);

            column.setText(titles[i]);
            column.setWidth(bounds[i]);
            column.setResizable(true);

            //Setting the right sorter
            column.addSelectionListener(new SelectionAdapter() 
            {
                @Override
                public void widgetSelected(SelectionEvent e) 
                {
                    tableSorter.setColumn(index);
                    final TableViewer viewer = _tableViewer;
                    int dir = viewer .getTable().getSortDirection();
                    if (viewer.getTable().getSortColumn() == column) 
                    {
                        dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
                    } 
                    else 
                    {
                        dir = SWT.UP;
                    }
                    viewer.getTable().setSortDirection(dir);
                    viewer.getTable().setSortColumn(column);
                    viewer.refresh();
                }
            });

        }
        
        _tableViewer.setContentProvider(new ContentProviderImpl());
        _tableViewer.setLabelProvider(new LabelProviderImpl());
        _tableViewer.setSorter(tableSorter);
        
        //Side Buttons
        Composite buttonsComposite = _toolkit.createComposite(tableAndButtonsComposite);
        gridData = new GridData(SWT.FILL, SWT.FILL, false, true);
        buttonsComposite.setLayoutData(gridData);
        buttonsComposite.setLayout(new GridLayout());
        
        final Button viewSelectedMsgButton = _toolkit.createButton(buttonsComposite, "View Message Content ...", SWT.PUSH);
        viewSelectedMsgButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        viewSelectedMsgButton.setEnabled(false);
        viewSelectedMsgButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                if (_table.getSelectionIndex() == -1)
                {
                    return;
                }
                
                viewMessageContent();
            }
        });
        
        final Button moveMessagesButton = _toolkit.createButton(buttonsComposite, "Move Message(s) ...", SWT.PUSH);
        moveMessagesButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        moveMessagesButton.setEnabled(false);
        moveMessagesButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                if (_table.getSelectionIndex() == -1)
                {
                    return;
                }
                
                moveMessages(moveMessagesButton.getShell());
            }
        });
                
        final Button clearQueueButton = _toolkit.createButton(buttonsComposite, "Clear Queue", SWT.PUSH);
        clearQueueButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        clearQueueButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                int response = ViewUtility.popupOkCancelConfirmationMessage("Clear Queue", 
                        "Clear queue ?");
                if (response == SWT.OK)
                {
                    try
                    {
                        _qmb.clearQueue();
                        ViewUtility.operationResultFeedback(null, "Queue cleared", null);
                    }
                    catch (Exception e2)
                    {
                        ViewUtility.operationFailedStatusBarMessage("Error clearing Queue");
                        MBeanUtility.handleException(_mbean, e2);
                    }

                    refresh(_mbean);;
                }
            }
        });
        
        _toolkit.createLabel(messagesGroup, "Message Header: ");
        
        //Redelivered status and header
        Composite headerEtcComposite = _toolkit.createComposite(messagesGroup);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        headerEtcComposite.setLayoutData(gridData);
        headerEtcComposite.setLayout(new GridLayout());
        
        final Text headerText = new Text(headerEtcComposite, SWT.WRAP | SWT.BORDER );
        headerText.setText("Select a message to view its header.");
        headerText.setEditable(false);
        data = new GridData(SWT.LEFT, SWT.TOP, false, false);
        data.minimumHeight = 230;
        data.heightHint = 230;
        data.minimumWidth = 500;
        data.widthHint = 500;
        headerText.setLayoutData(data);
        
        Composite redeliveryComposite = _toolkit.createComposite(headerEtcComposite);
        redeliveryComposite.setLayout(new GridLayout(2,false));
        data = new GridData(SWT.LEFT, SWT.FILL, false, false);
        data.minimumWidth = 150;
        data.widthHint = 150;
        redeliveryComposite.setLayoutData(data);
        
        _toolkit.createLabel(redeliveryComposite, "Redelivered: ");
        final Text redeliveredText = new Text(redeliveryComposite, SWT.BORDER);
        redeliveredText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        redeliveredText.setText("-");
        redeliveredText.setEditable(false);
        
        //listener for double clicking to view message content
        _table.addMouseListener(new MouseListener()                                              
        {
            // MouseListener implementation
            public void mouseDoubleClick(MouseEvent event)
            {
                viewMessageContent();
            }

            public void mouseDown(MouseEvent e){}
            public void mouseUp(MouseEvent e){}
        });
        
        //selection listener to enable and disable buttons, update header and redelivered info
        _tableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
            public void selectionChanged(SelectionChangedEvent evt)
            {
                int selectionIndex = _table.getSelectionIndex();

                if (selectionIndex == -1)
                {
                    headerText.setText("Select a message to view its header.");
                    redeliveredText.setText("-");
                    viewSelectedMsgButton.setEnabled(false);
                    moveMessagesButton.setEnabled(false);
                    
                    return;
                }
                else
                {   
                    moveMessagesButton.setEnabled(true);
                    
                    final CompositeData selectedMsg = (CompositeData)_table.getItem(selectionIndex).getData();
                    Boolean redelivered = (Boolean) selectedMsg.get(MSG_REDELIVERED);
                    redeliveredText.setText(redelivered.toString());

                    String[] msgHeader = (String[]) selectedMsg.get(MSG_HEADER);
                    headerText.setText("");
                    for(String s: msgHeader)
                    {
                        headerText.append(s + "\n");
                    }
                }
                
                if (_table.getSelectionCount() > 1)
                {
                    viewSelectedMsgButton.setEnabled(false);
                }
                else
                {
                    viewSelectedMsgButton.setEnabled(true);
                }

            }
        });

    }

    
    /**
     * Content Provider class for the table viewer
     */
    private class ContentProviderImpl  implements IStructuredContentProvider
    {
        
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
            
        }
        
        public void dispose()
        {
            
        }
        
        public Object[] getElements(Object parent)
        {
            Collection<Object> rowCollection = ((TabularDataSupport) parent).values();
           
            return rowCollection.toArray();
        }
    }
    
    /**
     * Label Provider class for the table viewer
     */
    private class LabelProviderImpl extends LabelProvider implements ITableLabelProvider
    {
        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            switch (columnIndex)
            {
                case 0 : // msg id column 
                    return String.valueOf(((CompositeDataSupport) element).get(MSG_AMQ_ID));
                case 1 : // msg size column 
                    return String.valueOf(((CompositeDataSupport) element).get(MSG_SIZE));
                case 2 : // msg position in queue 
                    return String.valueOf(((CompositeDataSupport) element).get(MSG_QUEUE_POS));
                default :
                    return "-";
            }
        }
        
        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }
        
    }

    /**
     * Sorter class for the table viewer.
     *
     */
    public class TableSorter extends ViewerSorter
    {
        private int column;
        private static final int ASCENDING = 0;
        private static final int DESCENDING = 1;

        private int direction = DESCENDING;

        public TableSorter()
        {
            this.column = 0;
            direction = ASCENDING;
        }

        public void setColumn(int column)
        {
            if (column == this.column)
            {
                // Same column as last sort; toggle the direction
                direction = 1 - direction;
            }
            else
            {
                // New column; do an ascending sort
                this.column = column;
                direction = ASCENDING;
            }
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2)
        {
            CompositeData msg1 = (CompositeData) e1;
            CompositeData msg2 = (CompositeData) e2;
            
            int comparison = 0;
            switch(column)
            {
                case 0:
                    comparison = ((Long) msg1.get(MSG_AMQ_ID)).compareTo((Long)msg2.get(MSG_AMQ_ID));
                    break;
                case 1:
                    comparison = ((Long) msg1.get(MSG_SIZE)).compareTo((Long)msg2.get(MSG_SIZE));
                    break;
                case 2:
                    comparison = ((Long) msg1.get(MSG_QUEUE_POS)).compareTo((Long)msg2.get(MSG_QUEUE_POS));
                    break;
                default:
                    comparison = 0;
            }
            // If descending order, flip the direction
            if(direction == DESCENDING)
            {
                comparison = -comparison;
            }
            return comparison;
        }
    }
    
    private void viewMessageContent()
    {
        int selectionIndex = _table.getSelectionIndex();

        if (selectionIndex != -1)
        {
            try
            {
                final CompositeData selectedMsg = (CompositeData)_table.getItem(selectionIndex).getData();
                Long msgId = (Long) selectedMsg.get(MSG_AMQ_ID);

                Object result = _qmb.viewMessageContent(msgId);

                populateResults(result);
            }
            catch (Exception e3)
            {
                MBeanUtility.handleException(_mbean, e3);
            }
        }
    }
    
    private void populateResults(Object result)
    {
        Display display = Display.getCurrent();
        int width = 610;
        int height = 400;
        Shell shell = ViewUtility.createPopupShell(RESULT, width, height);
        shell.setImage(ApplicationRegistry.getImage(CONSOLE_IMAGE));
        ViewUtility.populateCompositeWithData(_toolkit, shell, result);
        
        shell.open();
        while (!shell.isDisposed())
        {
            if (!display.readAndDispatch())
            {
                display.sleep();
            }
        }
        shell.dispose();
    }
    
    private void moveMessages(final Shell parent)
    {
        final ArrayList<Long> rangeStarts = new ArrayList<Long>();
        final ArrayList<Long> rangeEnds = new ArrayList<Long>();

        gatherSelectedAMQMsgIDRanges(rangeStarts,rangeEnds);
        String rangeString = getRangesString(rangeStarts,rangeEnds);
        
        final Shell shell = ViewUtility.createModalDialogShell(parent, "Move Messages");

        Composite idComposite = _toolkit.createComposite(shell, SWT.NONE);
        idComposite.setBackground(shell.getBackground());
        idComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        idComposite.setLayout(new GridLayout());
        
        _toolkit.createLabel(idComposite,"Move message(s) with AMQ ID:").setBackground(shell.getBackground());
        _toolkit.createLabel(idComposite,rangeString).setBackground(shell.getBackground());

        Composite destinationComposite = _toolkit.createComposite(shell, SWT.NONE);
        destinationComposite.setBackground(shell.getBackground());
        destinationComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        destinationComposite.setLayout(new GridLayout(2,false));
        
        _toolkit.createLabel(destinationComposite,"To Queue:").setBackground(shell.getBackground());
        final Combo destinationCombo = new Combo(destinationComposite,SWT.NONE | SWT.READ_ONLY);
        destinationCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        Composite okCancelButtonsComp = _toolkit.createComposite(shell);
        okCancelButtonsComp.setBackground(shell.getBackground());
        okCancelButtonsComp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));
        okCancelButtonsComp.setLayout(new GridLayout(2,false));
        
        Button okButton = _toolkit.createButton(okCancelButtonsComp, "OK", SWT.PUSH);
        okButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        Button cancelButton = _toolkit.createButton(okCancelButtonsComp, "Cancel", SWT.PUSH);
        cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        
        List<String> queueList = ApplicationRegistry.getServerRegistry(_mbean).getQueueNames(_mbean.getVirtualHostName());
        queueList.remove(_mbean.getName());
        
        if(queueList.size() == 0)
        {
            destinationCombo.setItems(new String[]{"No other queues available"});
            okButton.setEnabled(false);
        }
        else
        {
            destinationCombo.setItems(queueList.toArray(new String[0]));
        }
        destinationCombo.select(0);
        
        okButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                String destQueue = destinationCombo.getItem(destinationCombo.getSelectionIndex()).toString();
                shell.dispose();

                try
                {
                    for(int i=0 ; i < rangeStarts.size() ; i++)
                    {
                        Long from = rangeStarts.get(i);
                        Long to = rangeEnds.get(i);
                        
                        _qmb.moveMessages(Long.valueOf(from), Long.valueOf(to), destQueue);
                    }
                    
                    ViewUtility.operationResultFeedback(null, "Messages moved", null);
                }
                catch (Exception e4)
                {
                    ViewUtility.operationFailedStatusBarMessage("Error moving messages");
                    MBeanUtility.handleException(_mbean, e4);
                }

                refresh(_mbean);
            }
        });

        cancelButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                shell.dispose();
            }
        });

        shell.setDefaultButton(okButton);
        shell.pack();
        shell.open();
    }
    
    private void gatherSelectedAMQMsgIDRanges(ArrayList<Long> starts, ArrayList<Long> ends)
    {
        SortedSet<Long> amqIDs = new TreeSet<Long>();
        
        for(Integer i : _table.getSelectionIndices())
        {
            CompositeData selectedMsg = (CompositeData)_table.getItem(i).getData();
            amqIDs.add((Long)selectedMsg.get(MSG_AMQ_ID));
        }
                
        //initialise the first range
        Long start = amqIDs.first();
        Long end = amqIDs.first();
        
        for(Long id : amqIDs)
        {
            if(id == amqIDs.first())
            {
                //skip first check, already initialised range
                continue;
            }
            
            if(id == end +1)
            {
                //part of previous range, append
                end = id;
            }
            else
            {
                //not in previous range, record existing start and end msg id values
                starts.add(start);
                ends.add(end);
                
                //begin new range with this msg id
                start = id;
                end = id;
            }
        }
        
        //record the last range created
        starts.add(start);
        ends.add(end);
    }
    
    private String getRangesString(ArrayList<Long> starts, ArrayList<Long> ends)
    {
        String idRangesString = new String("");

        for(int i=0 ; i < starts.size() ; i++)
        {
            long start = starts.get(i);
            long end = ends.get(i);
            
            if(i != 0)
            {
                idRangesString = idRangesString.concat(", ");
            }
            
            if(start == end)
            {
                idRangesString = idRangesString.concat(String.valueOf(starts.get(i)));
            }
            else
            {
                idRangesString = idRangesString.concat(starts.get(i) + "-" + ends.get(i));
            }
        }
        
        return idRangesString.concat(".");
    }
}
