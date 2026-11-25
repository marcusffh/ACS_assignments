package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

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
		} catch (BookStoreException ignored) {
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

	/**
	 * Helper method to get the current state of a single book in the store.
	 *
	 * @param isbn
	 *            the isbn
	 * @return the stock book
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public StockBook getStockBook(int isbn) throws BookStoreException {
		Set<Integer> isbnSet = new HashSet<Integer>();
		isbnSet.add(isbn);
		return storeManager.getBooksByISBN(isbnSet).get(0);
	}

	/**
	 * Helper method to rate a single book once.
	 *
	 * @param isbn
	 *            the isbn
	 * @param rating
	 *            the rating
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void rateBook(int isbn, int rating) throws BookStoreException {
		Set<BookRating> bookRatings = new HashSet<BookRating>();
		bookRatings.add(new BookRating(isbn, rating));
		client.rateBooks(bookRatings);
	}

	/**
	 * Helper method to check that a book has never been rated. Needed because
	 * equals() on books ignores the rating fields.
	 *
	 * @param isbn
	 *            the isbn
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void assertNotRated(int isbn) throws BookStoreException {
		StockBook book = getStockBook(isbn);
		assertEquals(0, book.getNumTimesRated());
		assertEquals(0, book.getTotalRating());
	}

	/**
	 * Tests basic rateBooks() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBook() throws BookStoreException {
		rateBook(TEST_ISBN, 4);

		StockBook book = getStockBook(TEST_ISBN);
		assertEquals(1, book.getNumTimesRated());
		assertEquals(4, book.getTotalRating());
		assertEquals(4.0f, book.getAverageRating(), 0.0001f); /*Need floating point accuracy otherwise fails */
	}

	/**
	 * Tests that ratings accumulate and the average is totalRating /
	 * numTimesRated.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBookMultipleTimes() throws BookStoreException {
		// Separate calls, since BookRating.equals() only compares ISBNs, so two
		// ratings of the same book would collapse into one in a single set.
		rateBook(TEST_ISBN, 5);
		rateBook(TEST_ISBN, 2);

		StockBook book = getStockBook(TEST_ISBN);
		assertEquals(2, book.getNumTimesRated());
		assertEquals(7, book.getTotalRating());
		assertEquals(3.5f, book.getAverageRating(), 0.0001f);
	}
	/**
	 * Tests that a negative rating is rejected.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBookNegativeRating() throws BookStoreException {
		try {
			rateBook(TEST_ISBN, -1);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that a rating above 5 is rejected.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBookRatingTooHigh() throws BookStoreException {
		try {
			rateBook(TEST_ISBN, 6);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that one invalid rating makes the whole call fail, so the valid
	 * rating in the same set is not applied either (all-or-nothing).
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBooksAllOrNothingInvalidRating() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		Set<BookRating> bookRatings = new HashSet<BookRating>();
		bookRatings.add(new BookRating(TEST_ISBN, 3)); // valid
		bookRatings.add(new BookRating(TEST_ISBN + 1, 10)); // invalid rating

		try {
			client.rateBooks(bookRatings);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
		assertNotRated(TEST_ISBN + 1);
	}

	/**
	 * Tests that books with invalid ISBNs cannot be rated (again all-or-nothing).
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBooksInvalidISBN() throws BookStoreException {
		Set<BookRating> bookRatings = new HashSet<BookRating>();
		bookRatings.add(new BookRating(TEST_ISBN, 3)); // valid
		bookRatings.add(new BookRating(-1, 3)); // invalid

		try {
			client.rateBooks(bookRatings);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that books can only be rated if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBooksNonExistingISBN() throws BookStoreException {
		Set<BookRating> bookRatings = new HashSet<BookRating>();
		bookRatings.add(new BookRating(TEST_ISBN, 3)); // valid
		bookRatings.add(new BookRating(100000, 3)); // not in store

		try {
			client.rateBooks(bookRatings);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that rateBooks() rejects a null set with a BookStoreException.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBooksNullInput() throws BookStoreException {
		try {
			client.rateBooks(null);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that rating an empty set is allowed and changes nothing.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRateBooksEmptySet() throws BookStoreException {
		client.rateBooks(new HashSet<BookRating>());

		assertNotRated(TEST_ISBN);
	}

	/**
	 * Tests that getTopRatedBooks() returns the highest rated books, best first.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);
		addBooks(TEST_ISBN + 2, NUM_COPIES);

		rateBook(TEST_ISBN, 3);
		rateBook(TEST_ISBN + 1, 5);
		rateBook(TEST_ISBN + 2, 1);

		List<Book> topRated = client.getTopRatedBooks(2);

		assertEquals(2, topRated.size());
		assertEquals(TEST_ISBN + 1, topRated.get(0).getISBN());
		assertEquals(TEST_ISBN, topRated.get(1).getISBN());
	}

	/**
	 * Tests that the ranking uses the average rating and not the total rating.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksUsesAverage() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		// Total 6 but average 2.
		rateBook(TEST_ISBN, 2);
		rateBook(TEST_ISBN, 2);
		rateBook(TEST_ISBN, 2);

		// Total 4 but average 4.
		rateBook(TEST_ISBN + 1, 4);

		List<Book> topRated = client.getTopRatedBooks(1);

		assertEquals(1, topRated.size());
		assertEquals(TEST_ISBN + 1, topRated.get(0).getISBN());
	}

	/**
	 * Tests that unrated books (numTimesRated = 0) do not cause a division by
	 * zero, and that getTopRatedBooks() still works when nothing is rated.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksNoRatings() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);

		List<Book> topRated = client.getTopRatedBooks(2);

		assertEquals(2, topRated.size());
	}

	/**
	 * Tests that a rated book, even one rated 0, is ranked before unrated books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksRatedBeforeUnrated() throws BookStoreException {
		addBooks(TEST_ISBN + 1, NUM_COPIES);
		addBooks(TEST_ISBN + 2, NUM_COPIES);

		rateBook(TEST_ISBN + 2, 0);

		List<Book> topRated = client.getTopRatedBooks(1);

		assertEquals(1, topRated.size());
		assertEquals(TEST_ISBN + 2, topRated.get(0).getISBN());
	}

	/**
	 * Tests that asking for more books than the store has returns all books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksMoreThanAvailable() throws BookStoreException {
		rateBook(TEST_ISBN, 4);

		List<Book> topRated = client.getTopRatedBooks(10);

		assertEquals(1, topRated.size());
		assertEquals(TEST_ISBN, topRated.get(0).getISBN());
	}

	/**
	 * Tests that asking for zero books returns an empty list.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksZero() throws BookStoreException {
		rateBook(TEST_ISBN, 4);

		List<Book> topRated = client.getTopRatedBooks(0);

		assertEquals(0, topRated.size());
	}

	/**
	 * Tests that asking for a negative number of books is rejected.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksNegative() throws BookStoreException {
		try {
			client.getTopRatedBooks(-1);
			fail();
		} catch (BookStoreException ex) {
			;
		}
	}

	/**
	 * Tests that getTopRatedBooks() on an empty store returns an empty list.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooksEmptyStore() throws BookStoreException {
		storeManager.removeAllBooks();

		List<Book> topRated = client.getTopRatedBooks(5);

		assertEquals(0, topRated.size());
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
