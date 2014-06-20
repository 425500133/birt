

package org.eclipse.birt.report.engine.emitter.pptx;

import java.awt.Color;
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.birt.report.engine.emitter.pptx.util.PPTXUtil;
import org.eclipse.birt.report.engine.nLayout.area.IArea;
import org.eclipse.birt.report.engine.nLayout.area.IContainerArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.CellArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.RowArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.TableArea;
import org.eclipse.birt.report.engine.nLayout.area.style.BackgroundImageInfo;
import org.eclipse.birt.report.engine.nLayout.area.style.BorderInfo;
import org.eclipse.birt.report.engine.nLayout.area.style.BoxStyle;
import org.eclipse.birt.report.engine.nLayout.area.style.DiagonalInfo;
import org.eclipse.birt.report.engine.ooxml.writer.OOXmlWriter;

public class TableWriter
{

	private static final String RIGHTBORDERLINE = "a:lnR";
	private static final String LEFTBORDERLINE = "a:lnL";
	private static final String TOPBORDERLINE = "a:lnT";
	private static final String BOTTOMBORDERLINE = "a:lnB";	
	private int currentX;
	private int currentY;
	protected Stack<BoxStyle> rowStyleStack = new Stack<BoxStyle>();
	private final float scale; 
	private final PPTXRender render;
	private final PPTXPage graphics;
	private final PPTXCanvas canvas;
	protected OOXmlWriter writer;
	private static int TableIndex = 1;

	public TableWriter( PPTXRender render )
	{
		this.render = render;
		this.graphics = render.getGraphic( );
		this.canvas = render.getCanvas();
		this.writer = canvas.getWriter();
		scale = render.getScale();
		currentX = render.getCurrentX();
		currentY = render.getCurrentY();
	}

	public void outputTable( TableArea table )
	{
		drawTable( table );
	}

	protected void drawTable( TableArea table)
	{
		if ( table.needClip( ) )
		{
			render.startClip( table);

		}

		currentX += getX( table);
		currentY += getY( table );
		startTable(table);		
		Iterator<IArea> iter = table.getChildren( );
		while ( iter.hasNext( ) )
		{
			IArea child = iter.next( );
			drawRow( (RowArea) child);
		}
		if ( table.needClip( )) {
			//end clip...
		}
		updateRenderXY();
		if ( table.needClip( ))
		{
			graphics.endClip( );
		}
		currentX -= getX( table);
		currentY -= getY( table);
		endTable();
	}

	private void startTable( TableArea tablearea )
	{

		int X = PPTXUtil.convertToEnums( currentX );
		int Y = PPTXUtil.convertToEnums(  currentY );
		int width = PPTXUtil.convertToEnums( tablearea.getWidth( ));
		int height = PPTXUtil.convertToEnums( tablearea.getHeight( ));
		writer.openTag("p:graphicFrame");
		writer.openTag("p:nvGraphicFramePr");
		writer.openTag("p:cNvPr");
		writer.attribute("id", canvas.getPresentation( ).nextShapeId());
		writer.attribute("name", "Table "+TableIndex++); 
		writer.closeTag("p:cNvPr");
		writer.openTag("p:cNvGraphicFramePr");
		writer.openTag("a:graphicFrameLocks" );
		writer.attribute("noGrp", "1");
		writer.closeTag("a:graphicFrameLocks" );
		writer.closeTag("p:cNvGraphicFramePr");
		writer.openTag("p:nvPr");
		writer.closeTag("p:nvPr");
		writer.closeTag("p:nvGraphicFramePr");
		canvas.setPosition('p',X,Y,width, height);
		writer.openTag("a:graphic");
		writer.openTag("a:graphicData");
		writer.attribute("uri", "http://schemas.openxmlformats.org/drawingml/2006/table");
		writer.openTag("a:tbl");
		writeColumnsWidth(tablearea);
	}

	private void endTable()
	{
		writer.closeTag( "a:tbl" );
		writer.closeTag( "a:graphicData" );
		writer.closeTag( "a:graphic" );
		writer.closeTag( "p:graphicFrame" );
	}

	private void writeColumnsWidth( TableArea tablearea )
	{
		int numOfColumns = tablearea.getColumnCount( );
		int columnWidth = 0;
		writer.openTag( "a:tblGrid" );
		for( int i = 0; i< numOfColumns; i++)
		{
			columnWidth = PPTXUtil.convertToEnums(  tablearea.getCellWidth( i, i+1 ));
			writer.openTag( "a:gridCol" );
			writer.attribute( "w", columnWidth );
			writer.closeTag( "a:gridCol" );
		}
		writer.closeTag( "a:tblGrid" );
	}

	protected void drawRow( RowArea row)
	{
		currentX += getX( row);
		currentY += getY( row);
		rowStyleStack.push( row.getParent( ).getBoxStyle( ) );
		startRow( row ); //tags
		Iterator<IArea> iter = row.getChildren( );
		while ( iter.hasNext( ) )
		{
			IArea child = iter.next( );
			drawCell( (CellArea) child);
		}
		endRow();
		rowStyleStack.pop( );
		currentX -= getX( row);
		currentY -= getY( row);
	}

	private void startRow( RowArea row )
	{
		writer.openTag("a:tr");
		writer.attribute( "h", PPTXUtil.convertToEnums( row.getHeight( ) ) );

	}

	private void endRow( )
	{
		writer.closeTag( "a:tr" );

	}

	protected void drawCell( CellArea cell)
	{
		int rowid = cell.getRowID( );
		System.out.println("rowid: "+ rowid );
		int colid = cell.getColumnID( );
		System.out.println("colid: "+ colid );	
		currentX += getX( cell);
		currentY += getY( cell);
		startCell( cell );

		visitChildren(cell);

		endCell( cell );
		currentX -= getX( cell);
		currentY -= getY( cell);
	}


	/**
	 * start cell tag with all properties: styling
	 * @param cell
	 */
	private void startCell( CellArea cell )
	{
		writer.openTag( "a:tc" );

		int colspan = cell.getColSpan( );
		if( colspan > 1)
		{
			writer.attribute( "gridSpan", colspan );
		}
		int rowspan = cell.getRowSpan( );
		if( rowspan > 1)
		{
			writer.attribute( "rowSpan", rowspan );
		}	

	}

	private void endCell( CellArea cell )
	{
		writer.openTag( "a:tcPr" );
		//it is set zero since no way to retrieve margin except to set public CELL_DEFAULT		
		canvas.writeMarginProperties(0,0,0,0); 
		drawCellBox( cell);	
		writer.closeTag( "a:tcPr" );
		writer.closeTag( "a:tc" );

		// draw empty cells for colspan to fill
		int colspan = cell.getColSpan( );
		if( colspan > 1)
		{
			for(int emtpycell=1; emtpycell < colspan; emtpycell++)
			{
				writer.openTag( "a:tc" );
				writer.attribute( "hMerge", 1 );
				writer.openTag( "a:tcPr" );				
				//TODO: add emtpy cell properties:
				canvas.writeMarginProperties(0,0,0,0);
				writer.closeTag( "a:tcPr" );
				writer.closeTag( "a:tc" );
			}
		}
	}

	protected void visitChildren( IContainerArea container )
	{
		updateRenderXY();
		Iterator<IArea> iter = container.getChildren( );
		while ( iter.hasNext( ) )
		{
			IArea child = iter.next( );
			child.accept( render );
		}
		updateRenderXY();
	}

	/**
	 * draw the cells properties
	 * @param cell
	 */
	protected void drawCellBox( CellArea cell )
	{
		drawBorders( cell );		
		drawCellDiagonal( cell );

		BoxStyle style = cell.getBoxStyle( );		
		Color backgroundcolor = style.getBackgroundColor( );
		BackgroundImageInfo bgimginfo = style.getBackgroundImage( );

		if( !rowStyleStack.isEmpty( ) && (backgroundcolor == null || bgimginfo == null) )
		{
			BoxStyle rowStyle = rowStyleStack.peek( );
			if(rowStyle!=null) 
			{
				if(backgroundcolor == null )
				{
					backgroundcolor = rowStyle.getBackgroundColor( );
				}
				if(bgimginfo == null)
				{
					bgimginfo = rowStyle.getBackgroundImage( );
				}
			}			
		}

		//String imageUrl = EmitterUtil.getBackgroundImageUrl( style,reportDesign );

		// the container's start position (the left top corner of the
		// container)
		int startX = currentX;
		int startY = currentY;
		// the dimension of the container
		int width = getWidth( cell );
		int height = getHeight( cell );

		if ( backgroundcolor != null )
		{
			canvas.setColor( backgroundcolor );
		}
		if ( bgimginfo != null )
		{
			render.drawBackgroundImage( bgimginfo, startX, startY,width, height );
//			canvas.fillImage( bgimginfo., offsetX, offsetY, ToTile );
			//call internal function to only get imagefill: 
			//current implementation produce bug as it is inside shape tag
		}

	}

	protected void drawCellDiagonal( CellArea cell )
	{
		DiagonalInfo diagonalInfo = cell.getDiagonalInfo( );
		if ( diagonalInfo != null )
		{
			int startX = currentX;
			int startY = currentY;

			// the dimension of the container
			int width = getWidth( cell );
			int height = getHeight( cell );
			int dw = diagonalInfo.getDiagonalWidth( );
			int ds = diagonalInfo.getDiagonalStyle( );
			// support double style, use solid style instead.
			if ( ds == DiagonalInfo.BORDER_STYLE_DOUBLE )
			{
				ds = DiagonalInfo.BORDER_STYLE_SOLID;
			}
			switch ( diagonalInfo.getDiagonalNumber( ) )
			{
				case 2 :
					graphics.drawLine( startX + width/2 , startY , startX
							+ width, startY + height - dw/2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					graphics.drawLine( startX, startY + height/2 , startX
							+ width, startY + height - dw/2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					break;
				case 1 :
					graphics.drawLine( startX, startY + dw / 2, startX
							+ width, startY + height - dw / 2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					break;

				default :
					graphics.drawLine( startX, startY + dw / 2, startX
							+ width, startY + height - dw / 2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					graphics.drawLine( startX + width/2 , startY + dw/2, startX
							+ width, startY + height - dw/2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					graphics.drawLine( startX, startY + height/2 , startX
							+ width, startY + height - dw/2,
							getScaledValue( dw ), diagonalInfo.getDiagonalColor( ), ds );
					break;
			}

		}
	}

	/**
	 * assume leftborder is always draw
	 * @param container
	 */
	protected void drawBorders( IContainerArea container )
	{
		BoxStyle style = container.getBoxStyle( );
		if( style == null) return;

		BorderInfo baseborderinfo = style.getLeftBorder( );

		writeSingleBorder( LEFTBORDERLINE, baseborderinfo );		

		BorderInfo currentborderinfo = style.getRightBorder( );
		if( currentborderinfo != null)
		{
			writeSingleBorder( RIGHTBORDERLINE, currentborderinfo );
			baseborderinfo = currentborderinfo;
		}
		else
		{ //draw if border is empty:
			writeSingleBorder( RIGHTBORDERLINE, baseborderinfo );
		}

		currentborderinfo = style.getTopBorder( );
		if( currentborderinfo != null)
		{
			writeSingleBorder( TOPBORDERLINE, currentborderinfo);
			baseborderinfo = currentborderinfo;
		}
		else
		{ //draw if border is empty:
			writeSingleBorder( TOPBORDERLINE, baseborderinfo );			
		}

		currentborderinfo = style.getBottomBorder();
		if( currentborderinfo != null)
		{	
			writeSingleBorder( BOTTOMBORDERLINE, style.getBottomBorder());
		}
		else
		{ //draw if border is empty:
			writeSingleBorder( BOTTOMBORDERLINE, baseborderinfo );			
		}		

	}

	private void writeSingleBorder( String borderSide, BorderInfo borderinfo )
	{	
		if( borderinfo == null)
		{
			return;
		}
		writer.openTag( borderSide );
		int width = PPTXUtil.convertToEnums( borderinfo.getWidth( ));
		writer.attribute( "w", width );  //convert to EMU
		canvas.setColor( borderinfo.getColor( ) );
		writer.openTag( "a:prstDash");
		//TODO:  get the right style naming
		writer.attribute( "val", PPTXUtil.parseStyle( borderinfo.getStyle( )) );
		writer.closeTag( "a:prstDash" );
		writer.closeTag( borderSide );
	}

	private int getY(IContainerArea area) 
	{
		return getScaledValue( area.getY() );
	}

	private int getX(IContainerArea area) 
	{
		return getScaledValue( area.getX() );
	}

	private int getHeight(CellArea area) 
	{
		return getScaledValue( area.getHeight() );
	}

	private int getWidth(CellArea area) 
	{
		return getScaledValue( area.getWidth());
	}

	protected int getScaledValue( int value )
	{
		return (int) ( value * scale );
	}

	protected void updateRenderXY(){
		render.setCurrentX( currentX );
		render.setCurrentY( currentY );
	}

}
