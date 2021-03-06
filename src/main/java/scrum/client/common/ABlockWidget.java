package scrum.client.common;

import ilarkesto.core.logging.Log;
import ilarkesto.gwt.client.Gwt;
import scrum.client.ScrumScopeManager;
import scrum.client.dnd.BlockDndMarkerWidget;
import scrum.client.workspace.BlockCollapsedEvent;
import scrum.client.workspace.BlockExpandedEvent;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Base class for a block widget, which can be added to a <code>BlockWidgetList</code>.
 * 
 */
@SuppressWarnings("unchecked")
public abstract class ABlockWidget<O> extends AScrumWidget {

	private O object;
	private boolean extended;
	private BlockListWidget<O> list;

	private BlockHeaderWidget header;

	private FlowPanel outerPanel;
	private FlowPanel preHeaderPanel;
	private FlowPanel panel;
	private SimplePanel bodyWrapper;
	private BlockDndMarkerWidget dndMarkerTop = new BlockDndMarkerWidget();

	private boolean initializingExtension;
	private boolean initializedExtension;
	private Widget body;

	protected abstract void onInitializationHeader(BlockHeaderWidget header);

	protected abstract void onUpdateHeader(BlockHeaderWidget header);

	protected abstract Widget onExtendedInitialization();

	public ABlockWidget() {}

	@Override
	protected final Widget onInitialization() {
		header = new BlockHeaderWidget();
		header.initialize();
		if (ScrumScopeManager.isProjectScope() && getObject() instanceof AScrumGwtEntity) {
			header.appendCell(new UsersOnBlockWidget((AScrumGwtEntity) getObject()), null, false, false, null);
		}

		if (list.dndManager != null) list.dndManager.makeDraggable(this, header.getDragHandle());

		panel = Gwt.createFlowPanel("ABlockWidget", null, header);
		panel.add(header);

		outerPanel = Gwt.createFlowPanel("ABlockWidget-outer", null, dndMarkerTop, panel);

		dndMarkerTop.setActive(false);

		onInitializationHeader(header);
		header.addClickHandler(new SelectionClickHandler());

		return outerPanel;
	}

	@Override
	protected final void onUpdate() {
		onUpdateHeader(header);
		header.update();
		if (isExtended()) {
			ensureExtendedInitialized();
			onUpdateBody();
			if (bodyWrapper == null) {
				bodyWrapper = Gwt.createDiv("ABlockWidget-body", body);
				panel.add(bodyWrapper);
			}
		} else {
			if (bodyWrapper != null) {
				panel.remove(bodyWrapper);
				bodyWrapper = null;
			}
		}
		Gwt.update(preHeaderPanel);
	}

	protected void onUpdateBody() {
		Gwt.update(body);
	}

	private void ensureExtendedInitialized() {
		if (initializingExtension)
			throw new RuntimeException("Extension initializing. Don't call update() within onInitailization(): "
					+ toString());
		if (!initializedExtension) {
			if (initializingExtension) throw new RuntimeException("Extension already initializing: " + toString());
			initializingExtension = true;
			Log.DEBUG("Initializing extension: " + toString());
			body = onExtendedInitialization();
			initializedExtension = true;
			initializingExtension = false;
		}
	}

	protected final void setObject(O object) {
		assert this.object == null;
		assert object != null;
		this.object = object;
	}

	protected final O getObject() {
		return object;
	}

	// public Widget getBorderPanel() {
	// return panel;
	// }

	public void deactivateDndMarkers() {
		dndMarkerTop.setActive(false);
	}

	public void activateDndMarkerTop() {
		dndMarkerTop.setActive(true);
	}

	public final BlockListWidget<O> getList() {
		return list;
	}

	final void setList(BlockListWidget list) {
		this.list = list;
	}

	/**
	 * Indicates if the block is in extended-mode. This method should be called within the
	 * <code>build()</code>-method.
	 */
	public final boolean isExtended() {
		return extended;
	}

	/**
	 * This method is only called by BlockListWidget. To select a block on a BlockListWidget call
	 * <code>BlockListWidget.selectBlock(B block)</code> instead.
	 */
	final void setExtended(boolean extended) {
		if (this.extended == extended) return;
		this.extended = extended;

		if (extended) {
			new BlockExpandedEvent(getObject()).fireInCurrentScope();
			panel.addStyleName("ABlockWidget-extended");
		} else {
			new BlockCollapsedEvent(getObject()).fireInCurrentScope();
			panel.removeStyleName("ABlockWidget-extended");
		}

		update();
	}

	final void activate() {
		onActivation();
		scrollIntoView();
	}

	public void scrollIntoView() {
		getElement().scrollIntoView();
		header.getDragHandle().getElement().scrollIntoView();
	}

	protected void onActivation() {}

	@Override
	protected void onLoad() {
		super.onLoad();
		if (getList().isDndSorting()) {
			getList().dndManager.registerDropTarget(this);
		}
		if (extended) new BlockExpandedEvent(getObject()).fireInCurrentScope();

	}

	@Override
	protected void onUnload() {
		if (extended) new BlockCollapsedEvent(getObject()).fireInCurrentScope();

		if (list.dndManager != null) list.dndManager.unregisterDropTarget(this);
		super.onUnload();
	}

	public FlowPanel getPreHeaderPanel() {
		if (preHeaderPanel == null) {
			preHeaderPanel = new FlowPanel();
			outerPanel.insert(preHeaderPanel, 0);
		}
		return preHeaderPanel;
	}

	private class SelectionClickHandler implements ClickHandler {

		@Override
		public void onClick(ClickEvent event) {
			NativeEvent nativeEvent = event.getNativeEvent();
			boolean modifierDown = nativeEvent.getCtrlKey() || nativeEvent.getShiftKey() || nativeEvent.getAltKey();
			list.toggleExtension(getObject(), !modifierDown);
			event.stopPropagation();
		}

	}

	@Override
	public String toString() {
		return "[" + object + "]";
	}

}
