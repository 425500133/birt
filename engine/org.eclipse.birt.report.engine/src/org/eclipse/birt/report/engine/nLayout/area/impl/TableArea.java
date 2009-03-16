/***********************************************************************
 * Copyright (c) 2009 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Actuate Corporation - initial API and implementation
 ***********************************************************************/

package org.eclipse.birt.report.engine.nLayout.area.impl;

import java.awt.Color;
import java.util.ArrayList;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.content.IBandContent;
import org.eclipse.birt.report.engine.content.IColumn;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.ILabelContent;
import org.eclipse.birt.report.engine.content.IStyle;
import org.eclipse.birt.report.engine.content.ITableContent;
import org.eclipse.birt.report.engine.content.impl.ReportContent;
import org.eclipse.birt.report.engine.css.dom.StyleDeclaration;
import org.eclipse.birt.report.engine.css.engine.StyleConstants;
import org.eclipse.birt.report.engine.ir.DimensionType;
import org.eclipse.birt.report.engine.ir.EngineIRConstants;
import org.eclipse.birt.report.engine.ir.GridItemDesign;
import org.eclipse.birt.report.engine.layout.pdf.util.PropertyUtil;
import org.eclipse.birt.report.engine.nLayout.LayoutContext;
import org.eclipse.birt.report.engine.nLayout.area.IArea;
import org.eclipse.birt.report.engine.nLayout.area.ILayout;
import org.eclipse.birt.report.engine.nLayout.area.style.BackgroundImageInfo;
import org.eclipse.birt.report.engine.nLayout.area.style.BoxStyle;


public class TableArea extends RepeatableArea
{

	protected transient TableLayoutInfo layoutInfo;

	protected transient TableLayout layout;

	protected int startCol;
	protected int endCol;
	
	public TableArea( ContainerArea parent, LayoutContext context,
			IContent content )
	{
		super( parent, context, content );
	}


	TableArea( TableArea table )
	{
		super( table );
		layout = table.layout;
	}

	public boolean contains( RowArea row )
	{
		return children.contains( row );
	}

	public void addRow( RowArea row )
	{
		if ( layout != null )
		{
			layout.addRow( row, context.isFixedLayout( ) );
		}
	}
	
	public int getColumnCount( )
	{
		if ( content != null )
		{
			return ( (ITableContent) content ).getColumnCount( );
		}
		return 0;
	}
	
	protected boolean needRepeat( )
	{
		ITableContent table = (ITableContent) content;
		if ( table != null && table.isHeaderRepeat( ) )
		{
			IContent header = (IContent) table.getHeader( );
			if ( header != null && header.getChildren( ).size( ) > 0 )
			{
				return true;
			}
		}
		return false;
	}

	public TableArea cloneArea( )
	{
		return new TableArea( this );
	}

	public int getXPos( int columnID )
	{
		if ( layoutInfo != null )
		{
			return layoutInfo.getXPosition( columnID );
		}
		return 0;
	}
	
	public boolean isGridDesign()
	{
		if(content!=null)
		{
			Object gen = content.getGenerateBy( );
			return gen instanceof GridItemDesign;
		}
		return false;
	}
	
	

	protected void buildProperties( IContent content, LayoutContext context )
	{
		IStyle style = content.getStyle( );
		if ( style != null && !style.isEmpty( ) )
		{
			boxStyle = new BoxStyle( );
			IStyle cs = content.getComputedStyle( );
			Color color = PropertyUtil.getColor( cs
					.getProperty( IStyle.STYLE_BACKGROUND_COLOR ) );

			if ( color != null )
			{
				boxStyle.setBackgroundColor( color );
			}

			String url = style.getBackgroundImage( );
			if ( url != null )
			{
				boxStyle
						.setBackgroundImage( new BackgroundImageInfo(
								getImageUrl( url ),
								cs
										.getProperty( IStyle.STYLE_BACKGROUND_REPEAT ),
								getDimensionValue(
										cs
												.getProperty( IStyle.STYLE_BACKGROUND_POSITION_X ),
										width ),
								getDimensionValue(
										cs
												.getProperty( IStyle.STYLE_BACKGROUND_POSITION_Y ),
										width ) ) );

			}
			localProperties = new LocalProperties( );
			int maw = parent.getMaxAvaWidth( );

			localProperties.setMarginBottom( getDimensionValue( cs
					.getProperty( IStyle.STYLE_MARGIN_BOTTOM ), maw ) );
			localProperties.setMarginLeft( getDimensionValue( cs
					.getProperty( IStyle.STYLE_MARGIN_LEFT ), maw ) );
			localProperties.setMarginTop( getDimensionValue( cs
					.getProperty( IStyle.STYLE_MARGIN_TOP ), maw ) );
			localProperties.setMarginRight( getDimensionValue( cs
					.getProperty( IStyle.STYLE_MARGIN_RIGHT ), maw ) );
			if ( !isInInlineStacking )
			{
				pageBreakAfter =  cs	.getProperty( IStyle.STYLE_PAGE_BREAK_AFTER ) ;
				pageBreakInside =  cs.getProperty( IStyle.STYLE_PAGE_BREAK_INSIDE ) ;
				pageBreakBefore = cs.getProperty( IStyle.STYLE_PAGE_BREAK_BEFORE ) ;
			}
		}
		else
		{
			hasStyle = false;
			boxStyle = BoxStyle.DEFAULT;
			localProperties = LocalProperties.DEFAULT;
		}
		bookmark = content.getBookmark( );
		action = content.getHyperlinkAction( );
	}

	public void initialize( ) throws BirtException
	{
		calculateSpecifiedWidth( content );
		buildProperties( content, context );
		layoutInfo = resolveTableFixedLayout( content, context );
		width = layoutInfo.getTableWidth( );
		maxAvaWidth = layoutInfo.getTableWidth( );
		ITableContent tableContent = (ITableContent) content;
		int start = 0;
		int end = tableContent.getColumnCount( ) - 1;
		layout = new TableLayout( tableContent, layoutInfo, start, end );

		parent.add( this );
		// TODO addDummyColumnForRTL

		addCaption( ( (ITableContent) content ).getCaption( ) );
	}
	
	protected void addCaption( String caption ) throws BirtException
	{
		if ( caption == null || "".equals( caption ) ) //$NON-NLS-1$
		{
			return;
		}
		ReportContent report = (ReportContent)content.getReportContent( );
		ILabelContent captionLabel= report.createLabelContent( );
		captionLabel.setText( caption );
		StyleDeclaration style = new StyleDeclaration(report.getCSSEngine( ));
		style.setProperty( IStyle.STYLE_TEXT_ALIGN, IStyle.CENTER_VALUE );
		captionLabel.setInlineStyle( style );
		RowArea captionRow = new RowArea(getColumnCount( ));
		captionRow.setParent( this );
		captionRow.setWidth( width );
		CellArea captionCell = new CellArea();
		captionCell.setColSpan( getColumnCount( ) );
		captionCell.setWidth( width );
		captionCell.setMaxAvaWidth( width );
		captionRow.children.add( captionCell );
		ILayout layout = new BlockTextArea( captionCell, context, captionLabel );
		layout.layout( );
		int h = ((BlockContainerArea)layout).getAllocatedHeight();
		captionCell.setHeight( h );
		captionRow.setHeight( h );
		add( captionRow );
		repeatList.add( captionRow );
		update(captionRow);
	}
	
	protected boolean isInHeaderBand( )
	{
		if ( children.size( ) > 0 )
		{
			ContainerArea child = (ContainerArea) children
					.get( children.size( ) - 1 );
			IContent childContent = child.getContent( );
			if ( childContent != null )
			{
				if(childContent.getContentType( )==IContent.TABLE_GROUP_CONTENT)
				{
					return false;
				}
				IContent band = (IContent) childContent.getParent( );
				if ( band instanceof IBandContent )
				{
					int type = ( (IBandContent) band ).getBandType( );
					if ( type != IBandContent.BAND_HEADER)
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	
	public SplitResult split( int height, boolean force ) throws BirtException
	{
		SplitResult result = super.split( height, force );
		if ( result.getResult( ) != null )
		{
			TableArea tableResult = (TableArea) result.getResult( );
			int h = tableResult.layout.resolveAll( );
			if ( h > 0 )
			{
				tableResult.setHeight( tableResult.getHeight( ) + h );
			}
			tableResult.resolveBottomBorder( );
			resolveTopBorder( );
		}

		return result;
	}
	
	protected IArea getLastChild(ContainerArea container)
	{
		int count = container.getChildrenCount( );
		if(count>0)
		{
			return (IArea)container.getChild( count -1 );
		}
		return null;
	}
	
	protected RowArea getLastRow()
	{
		IArea child = getLastChild(this);
		while ( true )
		{
			if(child == null)
			{
				return null;
			}
			else
			{
				if(child instanceof RowArea)
				{
					return (RowArea)child;
				}
				else if(child instanceof ContainerArea)
				{
					child = getLastChild((ContainerArea)child);
				}
				else
				{
					return null;
				}
			}
		}
	}
	
	public void resolveBottomBorder()
	{
		RowArea lastRow = getLastRow( );
		if ( lastRow != null )
		{
			if ( lastRow.cells != null )
			{
				int bw = 0;
				for ( int i = 0; i < lastRow.cells.length; i++ )
				{
					if ( lastRow.cells[i] != null )
					{
						bw = Math.max( bw, layout
								.resolveBottomBorder( lastRow.cells[i] ) );
					}
				}
				if ( bw > 0 )
				{
					lastRow.setHeight( bw + lastRow.getHeight( ) );
					for ( int i = 0; i < lastRow.cells.length; i++ )
					{
						if ( lastRow.cells[i] != null )
						{
							if ( lastRow.cells[i] instanceof DummyCell )
							{
								// FIXME
								CellArea c = ( (DummyCell) lastRow.cells[i] )
										.getCell( );
							}
							else
							{
								lastRow.cells[i].setHeight( lastRow.cells[i]
										.getHeight( )
										+ bw );
							}
						}
					}
				}
			}
			// FIMXE update group/table height;
		}
	}

	
	public void resolveTopBorder()
	{
		
	}
	
	protected void addRows(ContainerArea container, TableLayout layout)
	{
		if(container instanceof RowArea)
		{
			layout.addRow( (RowArea)container, context.isFixedLayout( ) );
		}
		else
		{
			java.util.Iterator<IArea> iter = container.getChildren( );
			while(iter.hasNext( ))
			{
				ContainerArea child = (ContainerArea)iter.next( );
				addRows(child, layout);
			}
		}
	}
	
	

	public void close( ) throws BirtException
	{
		/*
		 * 1. resolve all unresolved cell 2. resolve table bottom border 3.
		 * update height of Root area 4. update the status of TableAreaLayout
		 */
		int borderHeight = 0;
		if ( layout != null )
		{
			int height = layout.resolveAll( );
			if ( 0 != height )
			{
				currentBP = currentBP + height;
			}
			borderHeight = layout.resolveBottomBorder( );
			layout.remove( this );
		}
		setHeight( currentBP + getOffsetY( ) + borderHeight );
		if ( parent != null )
		{
			checkPageBreak();
			parent.update( this );
		}
		finished = true;
	}

	public int getCellWidth( int startColumn, int endColumn )
	{
		if ( layoutInfo != null )
		{
			return layoutInfo.getCellWidth( startColumn, endColumn );
		}
		return 0;
	}

	public void resolveBorderConflict( CellArea cellArea, boolean isFirst )
	{
		if ( layout != null )
		{
			layout.resolveBorderConflict( cellArea, isFirst );
		}
	}

	private TableLayoutInfo resolveTableFixedLayout( IContent content,
			LayoutContext context )
	{
		assert ( parent != null );
		int parentMaxWidth = parent.getMaxAvaWidth( );
		int marginWidth = localProperties.getMarginLeft( )
				+ localProperties.getMarginRight( );
		return new TableLayoutInfo( (ITableContent) content, context,
				new ColumnWidthResolver( (ITableContent) content )
						.resolveFixedLayout( parentMaxWidth - marginWidth ) );
	}

	private class ColumnWidthResolver
	{

		ITableContent table;

		public ColumnWidthResolver( ITableContent table )
		{
			this.table = table;
		}

		/**
		 * Calculates the column width for the table. the return value should be
		 * each column width in point.
		 * 
		 * @param columns
		 *            The column width specified in report design.
		 * @param tableWidth
		 *            The suggested table width. If isTableWidthDefined is true,
		 *            this value is user defined table width; otherwise, it is
		 *            the max possible width for the table.
		 * @param isTableWidthDefined
		 *            The flag to indicate whether the table width has been
		 *            defined explicitly.
		 * @return each column width in point.
		 */
		protected int[] formalize( DimensionType[] columns, int tableWidth,
				boolean isTableWidthDefined )
		{
			ArrayList percentageList = new ArrayList( );
			ArrayList unsetList = new ArrayList( );
			ArrayList preFixedList = new ArrayList( );
			int[] resolvedColumnWidth = new int[columns.length];
			double total = 0.0f;
			int fixedLength = 0;
			for ( int i = 0; i < columns.length; i++ )
			{
				if ( columns[i] == null )
				{
					unsetList.add( new Integer( i ) );
				}
				else if ( EngineIRConstants.UNITS_PERCENTAGE.equals( columns[i]
						.getUnits( ) ) )
				{
					percentageList.add( new Integer( i ) );
					total += columns[i].getMeasure( );
				}
				else if ( EngineIRConstants.UNITS_EM.equals( columns[i]
						.getUnits( ) )
						|| EngineIRConstants.UNITS_EX.equals( columns[i]
								.getUnits( ) ) )
				{
					int len = getDimensionValue( table, columns[i],
							PropertyUtil.getDimensionValue( table
									.getComputedStyle( ).getProperty(
											StyleConstants.STYLE_FONT_SIZE ) ) );
					resolvedColumnWidth[i] = len;
					fixedLength += len;
				}
				else
				{
					int len = getDimensionValue( table, columns[i], tableWidth );
					resolvedColumnWidth[i] = len;
					preFixedList.add( new Integer( i ) );
					fixedLength += len;
				}
			}

			// all the columns have fixed width.
			if ( !isTableWidthDefined && unsetList.isEmpty( )
					&& percentageList.isEmpty( ) )
			{
				return resolvedColumnWidth;
			}

			if ( fixedLength >= tableWidth )
			{
				for ( int i = 0; i < unsetList.size( ); i++ )
				{
					Integer index = (Integer) unsetList.get( i );
					resolvedColumnWidth[index.intValue( )] = 0;
				}
				for ( int i = 0; i < percentageList.size( ); i++ )
				{
					Integer index = (Integer) percentageList.get( i );
					resolvedColumnWidth[index.intValue( )] = 0;
				}
				return resolvedColumnWidth;
			}

			if ( unsetList.isEmpty( ) )
			{
				if ( percentageList.isEmpty( ) )
				{
					int left = tableWidth - fixedLength;
					if ( !preFixedList.isEmpty( ) )
					{
						int delta = left / preFixedList.size( );
						for ( int i = 0; i < preFixedList.size( ); i++ )
						{
							Integer index = (Integer) preFixedList.get( i );
							resolvedColumnWidth[index.intValue( )] += delta;
						}
					}
				}
				else
				{
					float leftPercentage = ( ( (float) ( tableWidth - fixedLength ) ) / tableWidth ) * 100.0f;
					double ratio = leftPercentage / total;
					for ( int i = 0; i < percentageList.size( ); i++ )
					{
						Integer index = (Integer) percentageList.get( i );
						columns[index.intValue( )] = new DimensionType(
								columns[index.intValue( )].getMeasure( )
										* ratio, columns[index.intValue( )]
										.getUnits( ) );
						resolvedColumnWidth[index.intValue( )] = getDimensionValue(
								table, columns[index.intValue( )], tableWidth );
					}
				}
			}
			// unsetList is not empty.
			else
			{
				if ( percentageList.isEmpty( ) )
				{
					int left = tableWidth - fixedLength;
					int eachWidth = left / unsetList.size( );
					for ( int i = 0; i < unsetList.size( ); i++ )
					{
						Integer index = (Integer) unsetList.get( i );
						resolvedColumnWidth[index.intValue( )] = eachWidth;
					}
				}
				else
				{
					float leftPercentage = ( ( (float) ( tableWidth - fixedLength ) ) / tableWidth ) * 100.0f;
					if ( leftPercentage <= total )
					{
						double ratio = leftPercentage / total;
						for ( int i = 0; i < unsetList.size( ); i++ )
						{
							Integer index = (Integer) unsetList.get( i );
							resolvedColumnWidth[index.intValue( )] = 0;
						}
						for ( int i = 0; i < percentageList.size( ); i++ )
						{
							Integer index = (Integer) percentageList.get( i );
							columns[index.intValue( )] = new DimensionType(
									columns[index.intValue( )].getMeasure( )
											* ratio, columns[index.intValue( )]
											.getUnits( ) );
							resolvedColumnWidth[index.intValue( )] = getDimensionValue(
									table, columns[index.intValue( )],
									tableWidth );
						}
					}
					else
					{
						int usedLength = fixedLength;
						for ( int i = 0; i < percentageList.size( ); i++ )
						{
							Integer index = (Integer) percentageList.get( i );
							int width = getDimensionValue( table, columns[index
									.intValue( )], tableWidth );
							usedLength += width;
							resolvedColumnWidth[index.intValue( )] = width;

						}
						int left = tableWidth - usedLength;
						int eachWidth = left / unsetList.size( );
						for ( int i = 0; i < unsetList.size( ); i++ )
						{
							Integer index = (Integer) unsetList.get( i );
							resolvedColumnWidth[index.intValue( )] = eachWidth;
						}
					}
				}
			}
			return resolvedColumnWidth;
		}

		public int[] resolveFixedLayout( int maxWidth )
		{

			int columnNumber = table.getColumnCount( );
			DimensionType[] columns = new DimensionType[columnNumber];

			// handle visibility
			for ( int i = 0; i < columnNumber; i++ )
			{
				IColumn column = table.getColumn( i );
				DimensionType w = column.getWidth( );
				if ( startCol < 0 )
				{
					startCol = i;
				}
				endCol = i;
				if ( w == null )
				{
					columns[i] = null;
				}
				else
				{
					columns[i] = new DimensionType( w.getMeasure( ), w
							.getUnits( ) );

				}
			}
			if ( startCol < 0 )
				startCol = 0;
			if ( endCol < 0 )
				endCol = 0;

			boolean isTableWidthDefined = false;
			int specifiedWidth = getDimensionValue( table, table.getWidth( ),
					maxWidth );
			int tableWidth;
			if ( specifiedWidth > 0 )
			{
				tableWidth = specifiedWidth;
				isTableWidthDefined = true;
			}
			else
			{
				tableWidth = maxWidth;
				isTableWidthDefined = false;
			}
			return formalize( columns, tableWidth, isTableWidthDefined );
		}

		public int[] resolve( int specifiedWidth, int maxWidth )
		{
			assert ( specifiedWidth <= maxWidth );
			int columnNumber = table.getColumnCount( );
			int[] columns = new int[columnNumber];
			int columnWithWidth = 0;
			int colSum = 0;

			for ( int j = 0; j < table.getColumnCount( ); j++ )
			{
				IColumn column = table.getColumn( j );
				int columnWidth = getDimensionValue( table, column.getWidth( ),
						width );
				if ( columnWidth > 0 )
				{
					columns[j] = columnWidth;
					colSum += columnWidth;
					columnWithWidth++;
				}
				else
				{
					columns[j] = -1;
				}
			}

			if ( columnWithWidth == columnNumber )
			{
				if ( colSum <= maxWidth )
				{
					return columns;
				}
				else
				{
					float delta = colSum - maxWidth;
					for ( int i = 0; i < columnNumber; i++ )
					{
						columns[i] -= (int) ( delta * columns[i] / colSum );
					}
					return columns;
				}
			}
			else
			{
				if ( specifiedWidth == 0 )
				{
					if ( colSum < maxWidth )
					{
						distributeLeftWidth( columns, ( maxWidth - colSum )
								/ ( columnNumber - columnWithWidth ) );
					}
					else
					{
						redistributeWidth( columns, colSum - maxWidth
								+ ( columnNumber - columnWithWidth ) * maxWidth
								/ columnNumber, maxWidth, colSum );
					}
				}
				else
				{
					if ( colSum < specifiedWidth )
					{
						distributeLeftWidth( columns,
								( specifiedWidth - colSum )
										/ ( columnNumber - columnWithWidth ) );
					}
					else
					{
						if ( colSum < maxWidth )
						{
							distributeLeftWidth( columns, ( maxWidth - colSum )
									/ ( columnNumber - columnWithWidth ) );
						}
						else
						{
							redistributeWidth( columns, colSum - specifiedWidth
									+ ( columnNumber - columnWithWidth )
									* specifiedWidth / columnNumber,
									specifiedWidth, colSum );
						}
					}

				}

			}
			return columns;
		}

		private void redistributeWidth( int cols[], int delta, int sum,
				int currentSum )
		{
			int avaWidth = sum / cols.length;
			for ( int i = 0; i < cols.length; i++ )
			{
				if ( cols[i] < 0 )
				{
					cols[i] = avaWidth;
				}
				else
				{
					cols[i] -= (int) ( ( (float) cols[i] ) * delta / currentSum );
				}
			}

		}

		private void distributeLeftWidth( int cols[], int avaWidth )
		{
			for ( int i = 0; i < cols.length; i++ )
			{
				if ( cols[i] < 0 )
				{
					cols[i] = avaWidth;
				}
			}
		}
	}

	public class TableLayoutInfo
	{

		ITableContent tableContent;
		LayoutContext context;

		public TableLayoutInfo( ITableContent tableContent,
				LayoutContext context, int[] colWidth )
		{
			this.tableContent = tableContent;
			this.context = context;
			this.colWidth = colWidth;
			this.columnNumber = colWidth.length;
			this.xPositions = new int[columnNumber];
			this.tableWidth = 0;

			if ( tableContent.isRTL( ) ) // bidi_hcg
			{
				int parentMaxWidth = parent != null ? parent
						.getCurrentMaxContentWidth( ) : context.getMaxWidth( );
				for ( int i = 0; i < columnNumber; i++ )
				{
					xPositions[i] = parentMaxWidth - tableWidth - colWidth[i];
					tableWidth += colWidth[i];
				}
				if ( xPositions[columnNumber - 1] != 0 )
				{
					addDummyColumnForRTL( colWidth );
				}
			}
			else
			// ltr
			{
				for ( int i = 0; i < columnNumber; i++ )
				{
					xPositions[i] = tableWidth;
					tableWidth += colWidth[i];
				}
			}
		}

		public int getTableWidth( )
		{
			return this.tableWidth;
		}

		public int getXPosition( int index )
		{
			return xPositions[index];
		}

		/**
		 * get cell width
		 * 
		 * @param startColumn
		 * @param endColumn
		 * @return
		 */
		public int getCellWidth( int startColumn, int endColumn )
		{
			assert ( startColumn < endColumn );
			assert ( colWidth != null );
			int sum = 0;
			for ( int i = startColumn; i < endColumn; i++ )
			{
				sum += colWidth[i];
			}
			return sum;
		}

		protected int columnNumber;

		protected int tableWidth;
		/**
		 * Array of column width
		 */
		protected int[] colWidth = null;

		/**
		 * array of position for each column
		 */
		protected int[] xPositions = null;

		/**
		 * Creates a hidden column at X position 0.
		 * 
		 * @author bidi_hcg
		 */
		private void addDummyColumnForRTL( int[] colWidth )
		{
			this.colWidth = new int[columnNumber + 1];
			System.arraycopy( colWidth, 0, this.colWidth, 0, columnNumber );
			this.colWidth[columnNumber] = xPositions[columnNumber - 1];

			int[] newXPositions = new int[columnNumber + 1];
			System.arraycopy( xPositions, 0, newXPositions, 0, columnNumber );
			xPositions = newXPositions;
			xPositions[columnNumber] = 0;

			tableWidth += this.colWidth[columnNumber - 1];
			++columnNumber;
		}
	}

}
