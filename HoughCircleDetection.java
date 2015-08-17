import java.awt.*;
import java.io.*;
import javax.imageio.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.color.ColorSpace;

public class HoughCircleDetection
{
	public static void main(String[] args) throws Exception
	{
		//=================================================================================
		// READ IN IMAGE FROM FILE
		File input = new File(args[0]);
    	BufferedImage img = ImageIO.read(input);
    	
    	int w = img.getWidth();
   		int h = img.getHeight();
		//=================================================================================
		// CREATE COLOUR IMAGE
		BufferedImage imgColour = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);  
		Graphics g1 = imgColour.getGraphics();  
    	g1.drawImage(img, 0, 0, null);  
    	g1.dispose();  

    	// CREATE GREY SCALE IMAGE
    	BufferedImage imgGray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);  
    	Graphics g = imgGray.getGraphics();  
    	g.drawImage(img, 0, 0, null);  
    	g.dispose();  

    	// NORMALISE GREY SCALE IMAGE
    	getNormalised(imgGray,w,h);


    	File output1 = new File(stripExtension(input.getName())+"-1_grayscale"+getExtension(input.getName()) );
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output1);
		//=================================================================================
   		// GETS THE PIXEL VALUES AND CREATES A 2D ARRAY TO STORE THE PIXEL VALUES
   		int[][] pixels = new int[w][h];

   		for(int y=0;y<h;y++)
   		{
   			for(int x=0;x<w;x++)
   			{
   				Color c = new Color(imgGray.getRGB(x,y));
			    int red = c.getRed();
			    int green = c.getGreen();
			    int blue = c.getBlue();
			    pixels[x][y]=red;
   			}
   		}
 		//=================================================================================
 		// GAUSSIAN FILTER THE IMAGE

		// CREATES A KERNEL FOR THE GAUSSIAN FILTER
		int kernelSize = Integer.parseInt(args[1]);	// Kernel size
  		double sigma =Double.parseDouble(args[2]);	// sigma value
    	double[][] kernel = createGaussianFunction(kernelSize,kernelSize,sigma);

    	// GAUSSIAN FILTER
    	int[][] filt2Dimg = gaussianFilter(pixels, kernelSize,kernelSize, w, h, kernel, imgGray);
	    
	    File output2 = new File(stripExtension(input.getName())+"-2_gaussianfilter"+getExtension(input.getName()));
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output2);
 		//=================================================================================

		// APPLYING CANNY EDGE DECTION ALGORITHM
 		int[][] canX2Dimg = CannyXAlgorithm(filt2Dimg, w, h, kernel, imgGray);
   		int[][] canY2Dimg = CannyYAlgorithm(filt2Dimg, w, h, kernel, imgGray);

   		// GETS THE ANGLE (0,45,90,135)
   		double[][] angleimg = CannyAngleData(canX2Dimg, canY2Dimg, w,h, imgGray);

   		// GETS THE MAGNITUDE
   		int[][] magimg = CannyPixelData(canX2Dimg, canY2Dimg, w,h, imgGray);

   		File output5 = new File(stripExtension(input.getName())+"-3_cannyMagnitude"+getExtension(input.getName()));
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output5);

   		// APPLIES NON MAXIMUM SUPPRESSION
   		int[][] binImg = nonmaximumsuppression (angleimg, magimg, w,h, imgGray);

   		File output6 = new File(stripExtension(input.getName())+"-4_nonmaximumsuppression"+getExtension(input.getName()));
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output6);

   		// HYSTERESIS
   		int lowthreshhold = Integer.parseInt(args[3]);
   		int highthreshhold = Integer.parseInt(args[4]);

   		int[][] EdgeFinImg = Hysteresis(binImg, w,h, highthreshhold, lowthreshhold, imgGray);

   		File output7 = new File(stripExtension(input.getName())+"-5_Hysteresis"+getExtension(input.getName()));
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output7);
   		//=================================================================================
   		// HOUGH CIRCLE DETECTION

   		int radius = Integer.parseInt(args[5]);
   		double circleThreshold = Double.parseDouble(args[6]);

		int[][] CirclesDetectedImg2;
   		resetDrawing(w,h,imgGray);	// START DRAWING FROM SCRATCH

   		for(int i=radius; i<Math.min(w/2,h/2); i++)	// ACROSS radius given to max radius
   		{
   			CirclesDetectedImg2= houghCirlceDetect(EdgeFinImg, w,h, imgColour,imgGray, i,circleThreshold,true);
		}
   		File output9 = new File(stripExtension(input.getName())+"-7_CirclesDetected" +getExtension(input.getName()));
   		ImageIO.write(imgColour, getExtension(input.getName()).substring(1), output9);

   		File output10 = new File(stripExtension(input.getName())+"-6_accumulator" +getExtension(input.getName()));
   		ImageIO.write(imgGray, getExtension(input.getName()).substring(1), output10);

 		//=================================================================================

	}
	
	/**
	*	Gets name of file until the extension
	*/
	public static String stripExtension (String str) 
	{
        if (str == null) return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1) 
        	return str;

        return str.substring(0, pos);
    }

	/**
	*	Gets extension of the file
	*/
    public static String getExtension (String str) {
        if (str == null) return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1) 
        	return str;
        return str.substring(pos);
    }

    /**
    *	Normalises the image to fill the range of grey values from 0 to 255
    */
    public static void getNormalised(BufferedImage imgGray, int w, int h)
	{
		int max=0;
		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
		    	Color c = new Color(imgGray.getRGB(x,y));
			    int red = c.getRed();
			    if(red>max)
			    	max=red;
		    }
		}

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
		    	Color c = new Color(imgGray.getRGB(x,y));
			    int red = c.getRed();

			    int value =(int)(255*red/(double)max);
			   	Color newColor = new Color(value, value, value);		    	
		    	imgGray.setRGB(x,y,newColor.getRGB());
		    }
		}
	}
	
//================================================================================================================================================
//GAUSSIAN FILTER STARTS HERE
//================================================================================================================================================
	/**
	*	Creates a kernel for the gaussian filter
	*/
	public static double[][] createGaussianFunction(int sizeX, int sizeY, double sigma)
  	{
	    double[][] kernel =new double[sizeX][sizeY];
	    double total=0;
	    for(int y=(-1*sizeY/2);y<=sizeY/2;y++)
	    {
		    for(int x=(-1*sizeX/2);x<=sizeX/2;x++)
		    {
		    	kernel[x+(sizeX/2)][y+(sizeY/2)] = (1/((double)(2*Math.PI*Math.pow(sigma,2))))*Math.pow(Math.E,-(Math.pow(x,2)+Math.pow(y,2))/((double)( 2*Math.pow(sigma,2) )));//(1/(double)(2*Math.PI*sigma*sigma)) * Math.pow((Math.E),(-1)*((x*x+y*y)/(double)(2*sigma*sigma)));
		    	total+=kernel[x+(sizeX/2)][y+(sizeY/2)];
		    } 
	    } 
	    for(int y=(-1*sizeY/2);y<=sizeY/2;y++)
	    {
		    for(int x=(-1*sizeX/2);x<=sizeX/2;x++)
		    {
		    	kernel[x+(sizeX/2)][y+(sizeY/2)] = kernel[x+(sizeX/2)][y+(sizeY/2)]/((double)(total));
		    } 
	    } 
	    return kernel;
  	}

//================================================================================================================================================
  	/**
	*	Applies a gaussian filter to the image given
	*/
  	public static int[][] gaussianFilter(int[][] in, int kCols, int kRows, int cols, int rows, double[][] kernel, BufferedImage imgGray)
	{
		int[][] out = new int[cols][rows];
		int kCenterX = kCols / 2;
		int kCenterY = kRows / 2;

		for(int y=0; y < rows; ++y)  
		{
		    for(int x=0; x < cols; ++x)  
		    {
		        out[x][y] =0;
		        for(int yy=0; yy < kRows; ++yy) 
		        {
		        	for(int xx=0; xx < kCols; ++xx) 
                 	{
                 		int yD = y+yy-kCenterY;
                 		int xD = x+xx-kCenterX;

						if( yD >= 0 && yD < rows && xD >= 0 && xD < cols )
						{
	                 		out[x][y]+=kernel[xx][yy]*in[xD][yD];
	                 	}
                 	}     
		        }
		        Color newColor = new Color(out[x][y], out[x][y], out[x][y]);
            	imgGray.setRGB(x,y,newColor.getRGB());
		    }
		}
		return out;
	}
//================================================================================================================================================
//	CANNY EDGE DETECTION STARTS HERE
//================================================================================================================================================
	/**	Calculates a theta for each pixel
	*/
	public static double[][] CannyAngleData(int[][] canx, int[][] cany, int cols, int rows, BufferedImage imgGray)
	{
		double[][] out = new double[cols][rows];

		for(int y=0; y < rows; ++y)  
		{
		    for(int x=0; x < cols; ++x)  
		    {
		    	double theta;
		    	if(canx[x][y]!=0)
		    		theta = Math.atan(cany[x][y]/canx[x][y]);
		    	else
		    		theta =Math.PI*0.5;

		        if(theta<0)
		        {
		        	theta = theta+Math.PI;
		        }

		        //SIMPLIFY THETA

		        if((theta < Math.PI*0.25 )|| (theta>( Math.PI - Math.PI*0.25 )))
		        	theta =0;

		        if((theta > 0.625*Math.PI) && (theta < 0.875*Math.PI))
		        	theta = 0.75*Math.PI;

		        if((theta > 0.375*Math.PI) && (theta < 0.625*Math.PI))
		        	theta = 0.5*Math.PI;

		        if((theta > 0.125*Math.PI) && (theta < 0.375*Math.PI))
		        	theta = 0.25*Math.PI;

		    	out[x][y]=theta;
		    	


		    }
		}

		return out;
	}

	/**	Calculates the magnitute of the values from y sobel operator and x sobel operator
	*/
	public static int[][] CannyPixelData(int[][] canx, int[][] cany, int cols, int rows, BufferedImage imgGray)
	{
		int[][] out = new int[cols][rows];

		for(int y=0; y < rows; ++y)  
		{
		    for(int x=0; x < cols; ++x)  
		    {
		    	out[x][y]= (int) Math.sqrt(Math.pow(canx[x][y],2)+Math.pow(cany[x][y],2));

		    	// System.out.println(out[x][y]);
		    	int use=out[x][y];
		    	if(out[x][y]>255)
		    		use=255;

		    	Color newColor = new Color(use, use, use);
		    	imgGray.setRGB(x,y,newColor.getRGB());
		    }
		}
		return out;
	}

	/**	Applies Sobel operator in y direction
	*/
	public static int[][] CannyYAlgorithm(int[][] pixels, int cols, int rows, double[][] kernel, BufferedImage imgGray)
	{
		int[][] out = new int[cols][rows];
		int[][] canX = { {1, 0,-1} , { 2, 0,-2},{1, 0,-1} };

		for(int y=0; y < rows; ++y)  
		{
		    for(int x=0; x < cols; ++x)  
		    {
		    	out[x][y] =0;
		        for(int yy=0; yy < 3; ++yy) 
		        {
		        	for(int xx=0; xx < 3; ++xx) 
                 	{
                 		int yD = y+yy-1;
                 		int xD = x+xx-1;

						if( yD >= 0 && yD < rows && xD >= 0 && xD < cols )
						{
	                 		out[x][y]+=canX[xx][yy]*pixels[xD][yD];
	                 	}
                 	}     
		        }
		    }
		}

		return out;
	}

	/**	Applies Sobel operator in x direction
	*/
	public static int[][] CannyXAlgorithm(int[][] pixels, int cols, int rows, double[][] kernel, BufferedImage imgGray)
	{
		int[][] out = new int[cols][rows];
		int[][] canX = { {-1, -2, -1} , { 0, 0, 0 },{1, 2, 1} };

		for(int y=0; y < rows; ++y)  
		{
		    for(int x=0; x < cols; ++x)  
		    {
		    	out[x][y] =0;
		        for(int yy=0; yy < 3; ++yy) 
		        {
		        	for(int xx=0; xx < 3; ++xx) 
                 	{
                 		int yD = y+yy-1;
                 		int xD = x+xx-1;

						if( yD >= 0 && yD < rows && xD >= 0 && xD < cols )
						{
	                 		out[x][y]+=canX[xx][yy]*pixels[xD][yD];
	                 	}
                 	}     
		        }
		    }
		}

		return out;
	}

	/**	Applies nonmaximal suppression
	*/
	public static int[][] nonmaximumsuppression(double[][] canimg, int[][] magimg, int w, int h, BufferedImage imgGray)
	{
		int[][] out = new int[w][h];

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {		
		    	int a1=0;
				int a2=0;

		    	if(0==canimg[x][y])
				{	
					if((x-1)>=0)
						a1=magimg[x][y]-magimg[x-1][y];

					if((x+1)<w)
					 	a2=magimg[x][y]-magimg[x+1][y];

					if((a1>0)&&(a2>0))
					{
							out[x][y]=magimg[x][y];
						
					}
				}
				else 
				if(0.75*Math.PI==canimg[x][y])
				{
					if((x-1)>=0 && (y-1)>=0)
						a1=magimg[x][y]-magimg[x-1][y-1];

					if((x+1)<w && (y+1)<h)
					 	a2=magimg[x][y]-magimg[x+1][y+1];

					if((a1>0)&&(a2>0))
					{
							out[x][y]=magimg[x][y];
						
					}
				}
				else if(0.5*Math.PI==canimg[x][y])
				{
					
					if((y-1)>=0)
						a1=magimg[x][y]-magimg[x][y-1];

					if((y+1)<h)
					 	a2=magimg[x][y]-magimg[x][y+1];

					if((a1>0)&&(a2>0))
					{
							out[x][y]=magimg[x][y];
						
					}
				}
				else if(0.25*Math.PI==canimg[x][y])
				{

					if((x-1)>=0 && (y+1)<h)
						a1=magimg[x][y]-magimg[x-1][y+1];

					if((x+1)<w && (y-1)>=0)
					 	a2=magimg[x][y]-magimg[x+1][y-1];

					if((a1>0)&&(a2>0))
					{
							out[x][y]=magimg[x][y];
						
					}
				}
				int use=out[x][y];

		    	if(out[x][y]>0)
		    		use=255;
		    	else
		    		use=0;

		    	Color newColor = new Color(use, use, use);		    	
		    	imgGray.setRGB(x,y,newColor.getRGB());
			}
		}


		return out;
	}

	/*	Applies Hysteresis to non maximal suppression
	*/
	public static int[][] Hysteresis(int[][] magimg, int w, int h, int highthreshhold, int lowthreshhold, BufferedImage imgGray)
	{
		int[][] out = new int[w][h];

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {		
		    	if(magimg[x][y]>highthreshhold)
		    	{
		    		out[x][y]=255;
		    	}
		    	else if(magimg[x][y]<lowthreshhold)
		    	{
		    		out[x][y]=0;
		    	}
		    	else
		    	{
		    		boolean strong = false;
			    	for(int yy=Math.max(0,y-1); yy <= Math.min(h-1,y+1); ++yy) 
			        {
			        	for(int xx=Math.max(0,x-1); xx <=  Math.min(w-1,x+1); ++xx) 
	                 	{
	                 		if(magimg[xx][yy]>highthreshhold)
	                 		{
	                 			strong= true;
	                 			break;
	                 		}
	                 	}
	                }
	                if(strong)
	                {
	                	out[x][y]=255;
	                }
	                else
	                	out[x][y]=0;
           	 	}

           	 	Color newColor = new Color(out[x][y],out[x][y],out[x][y]);		    	
		    	imgGray.setRGB(x,y,newColor.getRGB());
		    }
		}
		return out;
	}
//================================================================================================================================================
//HOUGH CIRCLE DETECTION ALGORITHM METHODS START HERE
//================================================================================================================================================
	/**
	*		Detects circles given a certain radius
	*/
	public static int[][] houghCirlceDetect(int[][] edgeimg, int w, int h, BufferedImage img, BufferedImage imgGray, int radius,double circleThreshold,boolean colr)
	{
		int[][] out = new int[w][h];
		int count=0;

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
		    	if(edgeimg[x][y]==255)
			   	{
		    		getCircle(x,y, radius, img,out);
		    		//System.out.println(count);
			   	}
		    }
		}
		count = getCircle(w/2,h/2, radius, img,out);



		int max =0;
		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	

			   	if(out[x][y]>max)
			   		max=out[x][y];
		    }
		}

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
			   	int value=(int)(255*(out[x][y]/(double)(max)) );
			   	//added to see middle
			   	Color c = new Color(imgGray.getRGB(x,y));
			   	int current = c.getRed();
			   	if(value>current)
			   	{	
		    		Color newColor = new Color(value, value, value);		    	
		    		imgGray.setRGB(x,y,newColor.getRGB());
		    	}
		    }
		}

		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
		    	double factor = (4* radius *radius )/(double)((Math.min(w,x+radius)-Math.max(0,x-radius))*(Math.min(h,y+radius)-Math.max(0,y-radius)) )/*((Math.min(w,x+radius)-Math.max(0,x-radius))*(Math.min(h,y+radius)-Math.max(0,y-radius)) )*/;
			   	
			   	if(out[x][y]>(int)((count)/(double)(circleThreshold*factor)))
			   	{
		    		drawCircle(x,y, radius, img,255,colr);//out[x][y]);
				}
		    }
		}

		

		
		return out;
	}

	/**
	*	Draws a Cirlce onto the image
	*/
	public static void drawCircle(int centerX, int centerY, int r, BufferedImage imgGray, int value,boolean colr) {
		int d = (5 - r * 4)/4;
		int x = 0;
		int y = r;
 
		do {
			Color newColor;
			if(colr==false)
				newColor = new Color(value, value, value);		    	
			else
			{
				newColor = new Color(value, 0, 0);		    	
			}

			try{
			imgGray.setRGB(centerX + x, centerY + y, newColor.getRGB());
			}
			catch(Exception e){}

			try{			
			imgGray.setRGB(centerX + x, centerY - y, newColor.getRGB());
			}
			catch(Exception e){}

			try{
			imgGray.setRGB(centerX - x, centerY + y, newColor.getRGB());
			}
			catch(Exception e){}

			try{
			imgGray.setRGB(centerX - x, centerY - y, newColor.getRGB());
			}
			catch(Exception e){}

			try{
			imgGray.setRGB(centerX + y, centerY + x, newColor.getRGB());
			}
			catch(Exception e){}

			try{			
			imgGray.setRGB(centerX + y, centerY - x, newColor.getRGB());
			}
			catch(Exception e){}

			try{
			imgGray.setRGB(centerX - y, centerY + x, newColor.getRGB());
			}
			catch(Exception e){}

			try{			
			imgGray.setRGB(centerX - y, centerY - x, newColor.getRGB());
			}
			catch(Exception e){}
			

			if (d < 0) {
				d += 2 * x + 1;
			} else {
				d += 2 * (x - y) + 1;
				y--;
			}
			x++;
		} while (x <= y);
	}

	/*
	* 	Used for creating the acculator for the radius given
	*/
	private static int getCircle(int centerX, int centerY, int r, BufferedImage imgGray, int[][] out) 
	{
		int d = (5 - r * 4)/4;
		int x = 0;
		int y = r;

 		int count=0;

		do {

			try{
				out[centerX + x][centerY + y]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{
				out[centerX + x][centerY - y]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{
				out[centerX - x][centerY + y]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{			
				out[centerX - x][centerY - y]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{			
				out[centerX + y][centerY + x]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{			
				out[centerX + y][centerY - x]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{			
				out[centerX - y][centerY + x]+=1;
				count++;
			}
			catch(Exception e){}
			
			try{
				out[centerX - y][centerY - x]+=1;
				count++;
			}
			catch(Exception e){}
			
			
			if (d < 0) {
				d += 2 * x + 1;
			} else {
				d += 2 * (x - y) + 1;
				y--;
			}
			x++;
		} while (x <= y);
  		return count;

	}

//========================================================================================================================

	/*
	*	Resets image to blank image
	*/
	public static void resetDrawing(int w, int h, BufferedImage imgGray)
	{
		for(int y=0; y < h; ++y)  
		{
		    for(int x=0; x < w; ++x)  
		    {	
			   	Color newColor = new Color(0, 0, 0);		    	
		    	imgGray.setRGB(x,y,newColor.getRGB());
		    }
		}
	}

}