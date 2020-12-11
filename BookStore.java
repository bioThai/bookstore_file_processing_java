//bioThai
//This program reads bookstore inventory/shipping/order data from two input files and writes info to three output files.


import java.io.*;				//java.io package provides classes (eg, File, PrintWriter, FileWriter) for input/output operations
import java.util.Scanner;		//needed to read input from file
import java.time.LocalDate;		//needed to get current system date
import java.time.format.DateTimeFormatter;	//needed to format date

public class BookStore {

	//declare private class fields (variables)
	private Scanner statusInFile;
	private Scanner shipInFile;
	private String isbn_statusIn = "";	//ISBN of a book from statusInFile
	private String isbn_shipIn = "";	//ISBN of a book from shipInFile
	private int currentQty = 0;			//quantity of books currently in bookstore
	private int stockQty = 0;			//quantity of books that can be held in bookstore stock
	private int shippedQty = 0;			//quantity of books shipped by publisher to bookstore
	
	/********************************************************* 
	 * openInputFiles()
	 * @throws IOException
	 *********************************************************/
	private void openInputFiles() throws IOException
	{
		//create File objects that represent the files in the given paths (relative to current working directory)
		File inputFile1 = new File("CurrentStatusIn.txt");
		File inputFile2 = new File("Ship0916.txt");
		
		//create Scanner objs that can read items from the given File objs
		statusInFile = new Scanner(inputFile1);
		shipInFile = new Scanner(inputFile2);
		
		System.out.println("Input files successfully opened.");
	}
	
	/********************************************************* 
	 * closeInputFiles()
	 * @throws IOException
	 *********************************************************/
	private void closeInputFiles() throws IOException
	{
		statusInFile.close();
		shipInFile.close();
		
		System.out.println("Input files successfully closed.");
	}
	
	/********************************************************* 
	 * processInputFiles()
	 * Contains algorithm to compare and process data from the two given input files.
	 *********************************************************/
	private void processInputFiles() throws IOException
	{
		//local variable
		Boolean previousMatch = false;
		
		//move the scanner past the first line (the date) in statusInFile
		statusInFile.nextLine();
		
		readNextDataSet(statusInFile);
		readNextDataSet(shipInFile);
		
		//read lines from statusInfile until there are no lines left
		while (statusInFile.hasNext()) 
		{			
			if (isbn_statusIn.equals(isbn_shipIn))
			{
				currentQty += shippedQty;
				addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
				checkLowInventory();
				readNextDataSet(statusInFile);
				
				if (shipInFile.hasNext())
					readNextDataSet(shipInFile);
				else
					previousMatch = true;
				
				//if last line in statusInFile has been reached
				if (!statusInFile.hasNext())
				{
					if ((previousMatch) || (isbn_shipIn.compareTo(isbn_statusIn) > 0))
					{
						addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
						checkLowInventory();
					
						while (shipInFile.hasNext())
						{
							readNextDataSet(shipInFile);
							addToErrorFile(isbn_shipIn, shippedQty);
						}
					}
					else if (isbn_shipIn.compareTo(isbn_statusIn) < 0)
					{
						if (!previousMatch)
							addToErrorFile(isbn_shipIn, shippedQty);

						while (shipInFile.hasNext() && (isbn_shipIn.compareTo(isbn_statusIn) < 0))
						{
							readNextDataSet(shipInFile);
							addToErrorFile(isbn_shipIn, shippedQty);
						}
						if (isbn_statusIn.equals(isbn_shipIn))
						{
							currentQty += shippedQty;
							addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
							checkLowInventory();
						}
						while (shipInFile.hasNext() && (isbn_shipIn.compareTo(isbn_statusIn) > 0))
						{
							readNextDataSet(shipInFile);
							addToErrorFile(isbn_shipIn, shippedQty);
						}
					}
				}
			}
			//if ISBN from shipInFile > ISBN from statusInFile
			else if (isbn_shipIn.compareTo(isbn_statusIn) > 0)
			{
				addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
				checkLowInventory();
				readNextDataSet(statusInFile);
				
				//if last line in statusInFile has been reached
				if (!statusInFile.hasNext())
				{
					addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
					checkLowInventory();
					while (shipInFile.hasNext())
					{
						readNextDataSet(shipInFile);
						addToErrorFile(isbn_shipIn, shippedQty);
					}
				}
			}
			//if ISBN from shipInFile < ISBN from statusInFile
			else if (isbn_shipIn.compareTo(isbn_statusIn) < 0)
			{
				if (!previousMatch)
					addToErrorFile(isbn_shipIn, shippedQty);

				if (shipInFile.hasNext())
					readNextDataSet(shipInFile);
				else 
				{
					addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
					checkLowInventory();
					readNextDataSet(statusInFile);
					
					//if last line in statusInFile has been reached
					if (!statusInFile.hasNext())
					{
						addToStatusOutFile(isbn_statusIn, currentQty, stockQty);
						checkLowInventory();
					}
				}

			}
		}
		System.out.println("Input files successfully processed.");
	}
	
	/********************************************************* 
	 * readNextDataSet()
	 * Reads the next set of related data in a given file.
	 * Assigns each line read from a data set to its corresponding variable.
	 *********************************************************/
	private void readNextDataSet(Scanner scannerFile)
	{
		if (scannerFile.equals(statusInFile))
		{
			isbn_statusIn = statusInFile.nextLine();
			currentQty = Integer.parseInt(statusInFile.nextLine());
			stockQty = Integer.parseInt(statusInFile.nextLine());
		}
		else if (scannerFile.equals(shipInFile))
		{
			isbn_shipIn = shipInFile.nextLine();
			shippedQty = Integer.parseInt(shipInFile.nextLine());
		}
	}
	
	/********************************************************* 
	 * checkLowInventory()
	 * Checks if current quantity < 25% of stock quantity. 
	 * If yes, then adds appropriate isbn and quantity needed into order file.
	 * @throws IOException
	 *********************************************************/
	private void checkLowInventory() throws IOException
	{
		int neededQty = 0;
		if (currentQty < 0.25 * stockQty)
		{
			neededQty = stockQty - currentQty;
			addToOrderFile(isbn_statusIn, neededQty);
		}
	}

	/********************************************************* 
	 * addToStatusOutFile() 
	 * @throws IOException
	 *********************************************************/
	private void addToStatusOutFile(String isbn, int currQty, int stkQty) throws IOException
	{
		//create File object that represent the file in the given path
		File outputFile = new File("CurrentStatusOut.txt");
		
		//if output file doesn't exist
		if (!outputFile.exists())
		{	
			addDateTo(outputFile);
		}
		//create FileWriter obj and open an existing file to append data to
		FileWriter fwriter = new FileWriter(outputFile, true);
		
		//create PrintWriter obj that can write items into the given FileWriter obj
		PrintWriter statusOutFile = new PrintWriter(fwriter);		
		
		statusOutFile.println(isbn);
		statusOutFile.println(currQty);
		statusOutFile.println(stkQty);
		statusOutFile.close();
		
		System.out.println("Data set added to: " + outputFile.getName());
	}
	
	/********************************************************* 
	 * addToOrderFile()
	 * @throws IOException
	 *********************************************************/
	private void addToOrderFile(String isbn, int qtyNeeded) throws IOException
	{
		//create File object that represent the file in the given path
		File outputFile = new File("Order0916.txt");
		
		//if output file doesn't exist
		if (!outputFile.exists())
		{	
			addDateTo(outputFile);
		}
		//create FileWriter obj and open an existing file to append data to
		FileWriter fwriter = new FileWriter(outputFile, true);		
		PrintWriter orderFile = new PrintWriter(fwriter);
				
		orderFile.println(isbn);
		orderFile.println(qtyNeeded);
		orderFile.close();
		
		System.out.println("Data set added to: " + outputFile.getName());
	}
	
	/********************************************************* 
	 * addToErrorFile() 
	 * @throws IOException
	 *********************************************************/
	private void addToErrorFile(String isbn, int qtyShipped) throws IOException
	{
		//create File object that represent the file in the given path
		File outputFile = new File("Error0916.txt");		
		
		//create FileWriter obj and open a file to append data to (new file will be created if it doesn't exist yet)
		FileWriter fwriter = new FileWriter(outputFile, true);		
		
		PrintWriter errorFile = new PrintWriter(fwriter);
				
		errorFile.println(isbn);
		errorFile.println(qtyShipped);
		errorFile.close();
		
		System.out.println("Data set added to: " + outputFile.getName());
	}
	
	/********************************************************* 
	 * addDateTo(File fileObj) 
	 * If file is empty/new, date is added to beginning of file.
	 * Otherwise, date is appended on next line in file.
	 * @throws IOException
	 *********************************************************/
	private void addDateTo(File outFile) throws IOException
	{
		//create FileWriter obj and open a file to append data to (new file will be created if it doesn't exist yet)
		FileWriter fwriter = new FileWriter(outFile, true);		
		PrintWriter printWriterFile = new PrintWriter(fwriter);

		//add current system date to file associated with printWriter obj
		printWriterFile.println(getCurrentDate());
		printWriterFile.close();
	}
	
	/********************************************************* 
	 * getCurrentDate()
	 * Gets the current system date,
	 * formats it in MMddyyyy format,
	 * returns formatted date value.
	 *********************************************************/
	private String getCurrentDate()
	{
		//get current system date
		LocalDate date = LocalDate.now();
		
		//create a formatter with a specified pattern
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyyy");
		
		//format the current system date using the pattern assigned to the formatter
		return date.format(formatter);
	}
	
	/*********************************************************
	 * driver() 
	 * Handles the order of execution for the program.
	 * Catches thrown exceptions.
	 *********************************************************/
	private void driver()
	{
		try 
		{
			openInputFiles();
			processInputFiles();
			closeInputFiles();
		}
		//catch any IOException thrown by openInputFiles() or processInputFiles()
		catch (IOException exceptionObj)
		{
			//display default system error message
			System.out.println(exceptionObj.getMessage());
		}
	}
	
	/********************************************************* 
	 * main() 
	 * Instantiates an object of BookStore class.
	 * Calls the driver method.
	 *********************************************************/
	public static void main(String[] args) 
	{
		BookStore bookstoreObj = new BookStore();
		bookstoreObj.driver();
	}
}
