/*
 * This file is part of ADM, the Asterisk Desktop Manager.
 *
 * ADM is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * ADM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ADM; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.hamnett.adm.gui;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.hamnett.adm.Main;
import org.hamnett.adm.TrayAction;
import org.hamnett.adm.util.CommonDialogs;

public class SystemTray {
    private final Log logger = LogFactory.getLog(getClass());

    /**
     * Icon image that is used for the tray and as default icon for adm's windows.
     */
    private Image admIcon;
    private TrayItem trayItem;
    private Menu menu;
    private Map<String, PreferenceStore> stores;
    private String currentPreferenceStore;
    private MenuManager menuManager;
    private List menuContributions;

    public SystemTray(
            Map<String, PreferenceStore> stores,
            String currentPreferenceStore,
            List menuContributions
    ) {
        this.stores = stores;
        this.currentPreferenceStore = currentPreferenceStore;
        this.menuContributions = menuContributions;

        this.menuManager = new MenuManager();

        createComponents();
    }

    private void createComponents() {
        admIcon = ImageDescriptor.createFromFile(SystemTray.class, "icons/dazzl_bw.png").createImage();

        // set the dafault icon for all JFace windows that don't define one
        Window.setDefaultImage(admIcon);

        addTrayItem();
        addMenu();
    }

    private void addMenu() {
        Iterator itemIterator;

        // add contributed menu items (actions and contribution items)
        itemIterator = menuContributions.iterator();
        while (itemIterator.hasNext()) {
            Object item = itemIterator.next();

            if (item instanceof IContributionItem) {
                menuManager.add((IContributionItem) item);
            } else if (item instanceof IAction) {
                menuManager.add((IAction) item);
            } else {
                logger.warn("Unrecognized contribution of type " //$NON-NLS-1$
                        + item.getClass().getName() + " ignored."); //$NON-NLS-1$
            }

            //TODO is this the action to execute when clicking the tray?
            if (item instanceof TrayAction) {
                final IAction trayAction;

                trayAction = (IAction) item;
                trayItem.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event event) {
                        trayAction.run();
                        trayAction.runWithEvent(event);
                    }
                });
            }
        }

        //menuManager.add(new PreferencesAction());
        menuManager.add(new ConfigurationMenuManager(stores.keySet(), currentPreferenceStore));
        menuManager.add(new AboutAction());
        menuManager.add(new QuitAction(this));

        menu = menuManager.createContextMenu(Main.getShell());
    }

    private void addTrayItem() {
        Display display;
        Tray tray;
        Shell shell;

        display = Main.getDisplay();
        tray = display.getSystemTray();
        shell = Main.getShell();

        if (tray != null) {
            trayItem = new TrayItem(tray, SWT.NONE);
            trayItem.setToolTipText("Dazzl");  //$NON-NLS-1$
            trayItem.setImage(admIcon); //$NON-NLS-1$
            trayItem.addListener(SWT.MenuDetect, new Listener() {
                public void handleEvent(Event event) {
                    if (menu != null) {
                        menu.setVisible(true);
                    }
                }
            });
        } else {
            CommonDialogs.showError(Messages
                    .getString("SystemTray.error.no_system_tray"), null);
        }
    }

    public void run() {
        Display display;

        display = Main.getDisplay();
        while (!trayItem.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        display.dispose();
    }

    public void cleanup() {
        if (trayItem != null) {
            trayItem.dispose();
        }

        if (admIcon != null) {
            admIcon.dispose();
        }
    }

    public void setTrayIcon(Image icon) {
        trayItem.setImage(icon);
    }

    public void setTrayToolTipText(String text) {
        trayItem.setToolTipText(text);
    }
}