package com.acertainbookstore.client.tests;

import com.acertainbookstore.business.*;
import com.acertainbookstore.interfaces.*;
import com.acertainbookstore.utils.*;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class BookStoreConcurrencyTest {

    @Test
    public void testBuyAndAddCopiesConcurrency() throws InterruptedException {
        // Initialize the bookstore
        StockManager stockManager = new SingleLockConcurrentCertainBookStore();
        BookStore bookstore = (BookStore) stockManager;

        // Define a set of books with initial stock
        List<BookCopy> initialBooks = Arrays.asList(
                new BookCopy(1, 100),
                new BookCopy(2, 100)
        );

        try {
            stockManager.addBooks(Arrays.asList(
                    new Book(1, "Book A", "Author A", 100, 10),
                    new Book(2, "Book B", "Author B", 100, 10)
            ));
        } catch (BookStoreException e) {
            e.printStackTrace();
            fail("Failed to initialize bookstore with books");
        }

        // Define the clients
        Runnable client1 = () -> {
            try {
                for (int i = 0; i < 50; i++) { // Perform 50 buy operations
                    bookstore.buyBooks(Arrays.asList(new BookCopy(1, 1), new BookCopy(2, 1)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runnable client2 = () -> {
            try {
                for (int i = 0; i < 50; i++) { // Perform 50 addCopies operations
                    stockManager.addCopies(Arrays.asList(new BookCopy(1, 1), new BookCopy(2, 1)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Execute the clients concurrently
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(client1);
        executor.submit(client2);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Validate the final state of the bookstore
        try {
            List<Book> finalBooks = stockManager.getBooks();
            for (Book book : finalBooks) {
                assertEquals("Book stock mismatch for Book ID: " + book.getISBN(), 100, book.getNumCopies());
            }
        } catch (BookStoreException e) {
            e.printStackTrace();
            fail("Failed to retrieve books from the bookstore");
        }
    }
}
