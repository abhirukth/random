package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * {@link CertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class CertainBookStore implements BookStore, StockManager {

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<Integer, BookStoreBook> bookMap = null;

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public CertainBookStore() {

		// Constructors are not synchronized
		bookMap = new HashMap<>();
	}

	private synchronized void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookPrice < 0.0) { // Check if the price of the book is valid
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookMap.containsKey(isbn)) {// Check if the book is not in stock
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
		}
	}

	private synchronized void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

		if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
			throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
		}
	}

	private synchronized void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
	}

	private synchronized void validateISBNInStock(Integer ISBN) throws BookStoreException {
		if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
		}
		if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public synchronized void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all books are there first.
		for (StockBook book : bookSet) {
			try {
				validate(book);
			} catch (BookStoreException e) {
				System.out.println("Validation failed for book: " + book.getISBN() + " - " + e.getMessage());
				throw e; // Re-throw the exception after logging
			}
		}

		// Then add these books to the store.
		for (StockBook book : bookSet) {
			int isbn = book.getISBN();
			bookMap.put(isbn, new BookStoreBook(book));
			System.out.println("Book added: ISBN = " + isbn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public synchronized void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Validate all input
		for (BookCopy bookCopy : bookCopiesSet) {
			validate(bookCopy);
		}

		// Add copies and reset sale misses
		for (BookCopy bookCopy : bookCopiesSet) {
			BookStoreBook book = bookMap.get(bookCopy.getISBN());
			if (book != null) {
				book.addCopies(bookCopy.getNumCopies()); // Resets sales misses automatically
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public synchronized List<StockBook> getBooks() {
		Collection<BookStoreBook> bookMapValues = bookMap.values();

		return bookMapValues.stream().map(book -> book.immutableStockBook()).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public synchronized void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {

		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all books are there first.
		for (BookEditorPick editorPickArg : editorPicks) {
			validate(editorPickArg);
		}

		// Then set the editor pick.
		for (BookEditorPick editorPickArg : editorPicks) {
			bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public synchronized void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null || bookCopiesToBuy.isEmpty()) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int isbn;
		BookStoreBook book;
		Boolean saleMiss = false;

		Map<Integer, Integer> salesMisses = new HashMap<>();

		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			// Check whether the book is in stock.
			validate(bookCopyToBuy);
			isbn = bookCopyToBuy.getISBN();

			book = bookMap.get(isbn);
			// Check whether the number of book copy is enough for the request.
			if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
				// If we cannot sell the copies of the book, it is a miss.
				salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
				saleMiss = true;
			}
		}

		// We throw exception now since we want to see how many books in the
		// order incurred misses which is used by books in demand.
		if (saleMiss) {
			for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
				book = bookMap.get(saleMissEntry.getKey());
				book.addSaleMiss(saleMissEntry.getValue());
			}
			throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
		}

		// Then make the purchase.
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
	 * Set)
	 */
	public synchronized List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (Integer ISBN : isbnSet) {
			validateISBNInStock(ISBN);
		}

		// Return the set of books matching isbns in the validated set.
		return isbnSet.stream().map(isbn -> bookMap.get(isbn).immutableStockBook()).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public synchronized List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Check that all ISBNs that we rate are there to start with.
		for (Integer ISBN : isbnSet) {
			validateISBNInStock(ISBN);
		}

		return isbnSet.stream().map(isbn -> bookMap.get(isbn).immutableBook()).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public synchronized List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		// Get all books that are editor picks.
		List<BookStoreBook> listAllEditorPicks = bookMap.entrySet().stream().map(pair -> pair.getValue())
				.filter(book -> book.isEditorPick()).collect(Collectors.toList());

		// Find numBooks random indices of books that will be picked.
		Random rand = new Random();
		Set<Integer> tobePicked = new HashSet<>();
		int rangePicks = listAllEditorPicks.size();

		if (rangePicks <= numBooks) {

			// We need to add all books.
			for (int i = 0; i < listAllEditorPicks.size(); i++) {
				tobePicked.add(i);
			}
		} else {

			// We need to pick randomly the books that need to be returned.
			int randNum;

			while (tobePicked.size() < numBooks) {
				randNum = rand.nextInt(rangePicks);
				tobePicked.add(randNum);
			}
		}

		// Return all the books by the randomly chosen indices.
		return tobePicked.stream().map(index -> listAllEditorPicks.get(index).immutableBook())
				.collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public synchronized List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		// Handle empty store scenario
		if (bookMap.isEmpty()) {
			return Collections.emptyList();
		}

		var books = new ArrayList<>(bookMap.values());
		if (numBooks > books.size()) {
			throw new BookStoreException("More top rated books requested than available");
		}

		// Sort books by average rating in descending order
		Collections.sort(books, (a, b) -> Float.compare(b.getAverageRating(), a.getAverageRating()));

		// Return the top-rated books
		books = new ArrayList<>(books.subList(0, numBooks));
		return books.stream().map(book -> book.immutableBook()).collect(Collectors.toList());
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public synchronized List<StockBook> getBooksInDemand() throws BookStoreException {
		var books = new ArrayList<>(bookMap.values());
    return books.stream().filter(book -> book.hadSaleMiss()).map(book -> book.immutableStockBook()).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public synchronized void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		if (bookRating == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		// Validate all ratings first to maintain all-or-nothing semantics
		for (BookRating bookRatingElement : bookRating) {
			int isbn = bookRatingElement.getISBN();
			int rating = bookRatingElement.getRating();

			// Validate ISBN
			if (!bookMap.containsKey(isbn)) {
				throw new BookStoreException("Book with ISBN " + isbn + " not found.");
			}

			// Validate rating value
			if (rating < 0 || rating > 5) {
				throw new BookStoreException("Invalid rating: " + rating + ". Rating must be between 0 and 5.");
			}
		}

		// Apply the ratings after validation
		for (BookRating bookRatingElement : bookRating) {
			int isbn = bookRatingElement.getISBN();
			int rating = bookRatingElement.getRating();

			BookStoreBook book = bookMap.get(isbn);
			book.addRating(rating);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public synchronized void removeAllBooks() throws BookStoreException {
		bookMap.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public synchronized void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			}

			if (!bookMap.containsKey(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
			}
		}

		for (int isbn : isbnSet) {
			bookMap.remove(isbn);
		}
	}
}
