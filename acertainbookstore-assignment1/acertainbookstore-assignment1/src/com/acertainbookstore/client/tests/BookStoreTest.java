package com.acertainbookstore.client.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		storeManager.removeAllBooks();
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

  	/**
	 * Tests basic rateBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateDefaultBook() throws BookStoreException {
		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5));

		client.rateBooks(booksToRate);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getAverageRating() == 5);
	}

	/**
	 * Tests that books with invalid ISBNs cannot be rated.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5));
		booksToRate.add(new BookRating(-1, 5)); // invalid

		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be rated if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5));
		booksToRate.add(new BookRating(10000, 5)); // invalid

		try {
			client.rateBooks(booksToRate);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

    	/**
	 * Tests basic getTopRatedBooks() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(1, "Harry Potter and JUnit1", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 1, false));
		booksToAdd.add(new ImmutableStockBook(2, "Harry Potter and JUnit2", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 2, false));
		booksToAdd.add(new ImmutableStockBook(3, "Harry Potter and JUnit3", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 5, false));
		booksToAdd.add(new ImmutableStockBook(4, "Harry Potter and JUnit4", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 10, false));
		storeManager.addBooks(booksToAdd);

		var listBooks = client.getTopRatedBooks(2);

    assertTrue(listBooks.size() == 2);
		assertTrue(listBooks.get(0).getISBN() == 4 && listBooks.get(1).getISBN() == 3);
	}
  
  /**
	 * Tests getTopRatedBooks() with more K than available books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksMoreThanAvailable() throws BookStoreException {
    Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(1, "Harry Potter and JUnit1", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 1, false));
		booksToAdd.add(new ImmutableStockBook(2, "Harry Potter and JUnit2", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 2, false));
		booksToAdd.add(new ImmutableStockBook(3, "Harry Potter and JUnit3", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 5, false));
		booksToAdd.add(new ImmutableStockBook(4, "Harry Potter and JUnit4", "JK Unit", (float) 10, NUM_COPIES, 0, 1, 10, false));
		storeManager.addBooks(booksToAdd);
    List<StockBook> booksInStorePreTest = storeManager.getBooks();
    
		try {
      client.getTopRatedBooks(10);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	@Test
	public void testInvalidRatings() throws BookStoreException {
		// Prepare a set of books to rate with invalid ratings
		Set<BookRating> booksToRate = new HashSet<>();
		booksToRate.add(new BookRating(TEST_ISBN, -1)); // Invalid rating: below 0
		booksToRate.add(new BookRating(TEST_ISBN, 6));  // Invalid rating: above 5

		// Attempt to rate books with invalid ratings
		try {
			client.rateBooks(booksToRate);
			fail("Expected BookStoreException for invalid ratings");
		} catch (BookStoreException ex) {
			// Exception expected, test passes
		}

		// Verify that no changes occurred in the book's ratings
		StockBook book = storeManager.getBooks().stream()
				.filter(b -> b.getISBN() == TEST_ISBN)
				.findFirst()
				.orElse(null);

		assertTrue(book != null); // Ensure the book exists
		assertTrue(book.getTotalRating() == 0); // No ratings should be added
		assertTrue(book.getNumTimesRated() == 0); // No ratings should be counted
	}

	@Test
	public void testGetTopRatedBooksEmptyStore() throws BookStoreException {
		// Step 1: Clear all books from the store to ensure it is empty
		storeManager.removeAllBooks();

		// Step 2: Attempt to retrieve top-rated books
		List<Book> topRatedBooks = client.getTopRatedBooks(5);

		// Step 3: Verify the result is an empty list
		assertTrue(topRatedBooks.isEmpty());
	}

	@Test
	public void testBuyPartialAvailability() throws BookStoreException {
		// Clear the store to ensure no duplicates
		storeManager.removeAllBooks();

		// Step 1: Add books to the store
		Set<StockBook> booksToAdd = new HashSet<>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Test Book 1", "Author 1", 10.0f, 10, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Test Book 2", "Author 2", 15.0f, 5, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		// Step 2: Attempt to buy multiple books, where one book has an invalid ISBN
		Set<BookCopy> booksToBuy = new HashSet<>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 2)); // Valid
		booksToBuy.add(new BookCopy(TEST_ISBN + 2, 1)); // Invalid (not added to the store)

		try {
			client.buyBooks(booksToBuy);
			fail("Expected BookStoreException for partial availability.");
		} catch (BookStoreException ex) {
			// Exception expected
		}

		// Step 3: Verify no books were deducted from the store
		List<StockBook> booksInStore = storeManager.getBooks();
		assertTrue(booksInStore.stream()
				.filter(b -> b.getISBN() == TEST_ISBN)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Book not found in store"))
				.getNumCopies() == 10); // Stock intact for the first book

		assertTrue(booksInStore.stream()
				.filter(b -> b.getISBN() == TEST_ISBN + 1)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Book not found in store"))
				.getNumCopies() == 5); // Stock intact for the second book
	}

	@Test
	public void testBuyNegativeQuantity() throws BookStoreException {
		storeManager.removeAllBooks();
		// Step 1: Add valid books to the store
		Set<StockBook> booksToAdd = new HashSet<>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN, "Test Book 1", "Author 1", 10.0f, 10, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		// Step 2: Attempt to buy a negative number of copies
		Set<BookCopy> booksToBuy = new HashSet<>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1)); // Invalid quantity

		try {
			client.buyBooks(booksToBuy);
			fail("Expected BookStoreException for negative quantity.");
		} catch (BookStoreException ex) {
			// Exception expected
		}

		// Step 3: Verify that no books were deducted from the store
		StockBook bookInStore = storeManager.getBooks().stream()
				.filter(b -> b.getISBN() == TEST_ISBN)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Book not found in store"));

		assertTrue(bookInStore.getNumCopies() == 10); // Original stock intact
	}

	@Test
	public void testBuyEmptyBookSet() throws BookStoreException {
		try {
			// Attempt to buy with an empty set
			client.buyBooks(new HashSet<>());
			fail("Expected BookStoreException for empty book set.");
		} catch (BookStoreException ex) {
			// Assert exception message or type
			assertTrue(ex.getMessage().contains("NULL_INPUT") || ex instanceof BookStoreException);
		}
	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
