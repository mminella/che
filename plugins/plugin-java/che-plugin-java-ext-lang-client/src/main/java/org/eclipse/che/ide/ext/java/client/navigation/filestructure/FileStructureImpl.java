/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.java.client.navigation.filestructure;

import static com.google.common.collect.Iterables.all;
import static org.eclipse.che.ide.ui.smartTree.SelectionModel.Mode.SINGLE;

import com.google.common.base.Predicate;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.events.Event;
import java.util.Collections;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.ext.java.client.JavaExtension;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.navigation.factory.NodeFactory;
import org.eclipse.che.ide.ext.java.shared.dto.model.CompilationUnit;
import org.eclipse.che.ide.ui.smartTree.KeyboardNavigationHandler;
import org.eclipse.che.ide.ui.smartTree.NodeLoader;
import org.eclipse.che.ide.ui.smartTree.NodeStorage;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.data.Node;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.input.CharCodeWithModifiers;
import org.eclipse.che.ide.util.input.SignalEvent;
import org.eclipse.che.ide.util.input.SignalEventUtils;

/**
 * Implementation of {@link FileStructure} view.
 *
 * @author Valeriy Svydenko
 */
@Singleton
final class FileStructureImpl extends Window implements FileStructure {

  private static FileStructureImplUiBinder UI_BINDER = GWT.create(FileStructureImplUiBinder.class);

  @UiField(provided = true)
  final JavaLocalizationConstant locale;

  private final ActionManager actionManager;
  private final PresentationFactory presentationFactory;

  private final NodeFactory nodeFactory;
  private final Tree tree;
  private final KeyBindingAgent keyBindingAgent;
  @UiField DockLayoutPanel treeContainer;
  @UiField Label showInheritedLabel;
  private ActionDelegate delegate;
  private Predicate<Node> LEAFS = input -> input.isLeaf();

  @Inject
  public FileStructureImpl(
      NodeFactory nodeFactory,
      JavaLocalizationConstant locale,
      KeyBindingAgent keyBindingAgent,
      ActionManager actionManager,
      PresentationFactory presentationFactory) {
    super(false);
    this.nodeFactory = nodeFactory;
    this.locale = locale;
    this.actionManager = actionManager;
    this.presentationFactory = presentationFactory;
    this.keyBindingAgent = keyBindingAgent;

    setWidget(UI_BINDER.createAndBindUi(this));

    NodeStorage storage =
        new NodeStorage((NodeUniqueKeyProvider) item -> String.valueOf(item.hashCode()));
    NodeLoader loader = new NodeLoader(Collections.emptySet());
    tree = new Tree(storage, loader);
    tree.setAutoExpand(false);
    tree.getSelectionModel().setSelectionMode(SINGLE);

    KeyboardNavigationHandler handler =
        new KeyboardNavigationHandler() {
          @Override
          public void onEnter(NativeEvent evt) {
            hide();
          }
        };
    tree.addDomHandler(
        event -> {
          if (all(tree.getSelectionModel().getSelectedNodes(), LEAFS)) {
            hide();
          }
        },
        DoubleClickEvent.getType());

    handler.bind(tree);

    treeContainer.add(tree);

    tree.enableSpeedSearch(true);
  }

  /** {@inheritDoc} */
  @Override
  public void setStructure(CompilationUnit compilationUnit, boolean showInheritedMembers) {
    showInheritedLabel.setText(
        showInheritedMembers
            ? locale.hideInheritedMembersLabel()
            : locale.showInheritedMembersLabel());
    tree.getNodeStorage().clear();
    tree.getNodeStorage()
        .add(
            nodeFactory.create(
                compilationUnit.getTypes().get(0), compilationUnit, showInheritedMembers, false));
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    hide();
  }

  /** {@inheritDoc} */
  @Override
  public void show() {
    super.show(tree);
    if (!tree.getRootNodes().isEmpty()) {
      tree.getSelectionModel().select(tree.getRootNodes().get(0), false);
    }
    tree.expandAll();
  }

  @Override
  public void onClose() {
    tree.closeSpeedSearchPopup();
    this.hide();
  }

  /** {@inheritDoc} */
  @Override
  public void hide() {
    super.hide();
    delegate.onEscapeClicked();
  }

  /** {@inheritDoc} */
  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  protected void onKeyDownEvent(KeyDownEvent event) {
    handleKey(event);
  }

  @Override
  protected void onKeyPressEvent(KeyPressEvent event) {
    handleKey(event);
  }

  private void handleKey(KeyEvent<?> event) {
    SignalEvent signalEvent = SignalEventUtils.create((Event) event.getNativeEvent(), false);
    CharCodeWithModifiers keyBinding =
        keyBindingAgent.getKeyBinding(JavaExtension.JAVA_CLASS_STRUCTURE);
    if (signalEvent == null || keyBinding == null) {
      return;
    }
    int digest = CharCodeWithModifiers.computeKeyDigest(signalEvent);
    if (digest == keyBinding.getKeyDigest()) {
      Action action = actionManager.getAction(JavaExtension.JAVA_CLASS_STRUCTURE);
      if (action != null) {
        ActionEvent e = new ActionEvent(presentationFactory.getPresentation(action), actionManager);
        action.update(e);

        if (e.getPresentation().isEnabled()) {
          event.preventDefault();
          event.stopPropagation();
          action.actionPerformed(e);
        }
      }
    }
  }

  interface FileStructureImplUiBinder extends UiBinder<Widget, FileStructureImpl> {}
}
